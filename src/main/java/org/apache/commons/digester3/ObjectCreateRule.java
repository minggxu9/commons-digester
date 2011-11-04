package org.apache.commons.digester3;

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

import static java.util.Arrays.asList;
import static java.lang.String.format;

import static org.apache.commons.beanutils.ConvertUtils.convert;
import static org.apache.commons.beanutils.ConstructorUtils.getAccessibleConstructor;

import java.lang.reflect.Constructor;
import java.util.Formatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.xml.sax.Attributes;

/**
 * Rule implementation that creates a new object and pushes it onto the object stack. When the element is complete, the
 * object will be popped
 */
public class ObjectCreateRule
    extends Rule
{

    // ----------------------------------------------------------- Constructors

    /**
     * Construct an object create rule with the specified class name.
     *
     * @param className Java class name of the object to be created
     */
    public ObjectCreateRule( String className )
    {
        this( className, (String) null );
    }

    /**
     * Construct an object create rule with the specified class.
     *
     * @param clazz Java class name of the object to be created
     */
    public ObjectCreateRule( Class<?> clazz )
    {
        this( clazz.getName(), (String) null );
        this.clazz = clazz;
    }

    /**
     * Construct an object create rule with the specified class name and an optional attribute name containing an
     * override.
     *
     * @param className Java class name of the object to be created
     * @param attributeName Attribute name which, if present, contains an override of the class name to create
     */
    public ObjectCreateRule( String className, String attributeName )
    {
        this.className = className;
        this.attributeName = attributeName;
    }

    /**
     * Construct an object create rule with the specified class and an optional attribute name containing an override.
     *
     * @param attributeName Attribute name which, if present, contains an
     * @param clazz Java class name of the object to be created override of the class name to create
     */
    public ObjectCreateRule( String attributeName, Class<?> clazz )
    {
        this( clazz.getName(), attributeName );
        this.clazz = clazz;
    }

    // ----------------------------------------------------- Instance Variables

    /**
     * The attribute containing an override class name if it is present.
     */
    protected String attributeName = null;

    /**
     * The Java class of the object to be created.
     */
    protected Class<?> clazz = null;

    /**
     * The Java class name of the object to be created.
     */
    protected String className = null;

    /**
     * The constructor arguments - order is preserved by the LinkedHashMap
     *
     * @since 3.2
     */
    private final Map<String, Class<?>> constructorArguments = new LinkedHashMap<String, Class<?>>();

    // --------------------------------------------------------- Public Methods

    /**
     * Allows users specify constructor arguments <b>from attributes only</b>.
     *
     * @since 3.2
     */
    public void addConstructorArgument( String attibuteName, Class<?> type )
    {
        if ( attibuteName == null )
        {
            throw new IllegalArgumentException( "Parameter 'attibuteName' must not be null" );
        }
        if ( type == null )
        {
            throw new IllegalArgumentException( "Parameter 'type' must not be null" );
        }

        constructorArguments.put( attibuteName, type );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void begin( String namespace, String name, Attributes attributes )
        throws Exception
    {
        Class<?> clazz = this.clazz;

        if ( clazz == null )
        {
            // Identify the name of the class to instantiate
            String realClassName = className;
            if ( attributeName != null )
            {
                String value = attributes.getValue( attributeName );
                if ( value != null )
                {
                    realClassName = value;
                }
            }
            if ( getDigester().getLogger().isDebugEnabled() )
            {
                getDigester().getLogger().debug( format( "[ObjectCreateRule]{%s} New '%s'",
                                                         getDigester().getMatch(),
                                                         realClassName ) );
            }

            // Instantiate the new object and push it on the context stack
            clazz = getDigester().getClassLoader().loadClass( realClassName );
        }
        Object instance;
        if ( constructorArguments.isEmpty() )
        {
            if ( getDigester().getLogger().isDebugEnabled() )
            {
                getDigester().getLogger().debug( format( "[ObjectCreateRule]{%s} New '%s' using default empty constructor",
                                                         getDigester().getMatch(),
                                                         clazz.getName() ) );
            }

            instance = clazz.newInstance();
        }
        else
        {
            Class<?>[] parameterTypes = new Class<?>[constructorArguments.size()];
            Object[] initargs = new Object[constructorArguments.size()];

            int counter = 0;

            // prepare the arguments types with related values
            for ( Entry<String, Class<?>> argEntry : constructorArguments.entrySet() )
            {
                parameterTypes[counter] = argEntry.getValue();

                String argumentValueAsString = attributes.getValue( argEntry.getKey() );
                // ConvertUtils manages null values as well
                initargs[counter] = convert( argumentValueAsString, parameterTypes[counter] );

                counter++;
            }

            Constructor<?> constructor = getAccessibleConstructor( clazz, parameterTypes );

            if ( constructor == null )
            {
                throw new IllegalArgumentException( format( "[ObjectCreateRule]{%s} class '%s' doesn't have a Contructor with params %s",
                                                            getDigester().getMatch(),
                                                            clazz.getName(),
                                                            asList( parameterTypes ) ) );
            }

            // print out constructor debug
            if ( getDigester().getLogger().isDebugEnabled() )
            {
                Formatter formatter = new Formatter().format( "[ObjectCreateRule]{%s} New '%s' using constructor( ",
                                                              getDigester().getMatch(),
                                                              clazz.getName() );
                for ( int i = 0; i < initargs.length; i++ )
                {
                    formatter.format( "%s%s/%s", ( i > 0 ? ", " : "" ), initargs[i], parameterTypes[i].getName() );
                }
                formatter.format( " )" );
                getDigester().getLogger().debug( formatter.toString() );
            }

            instance = constructor.newInstance( initargs );
        }
        getDigester().push( instance );
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void end( String namespace, String name )
        throws Exception
    {
        Object top = getDigester().pop();
        if ( getDigester().getLogger().isDebugEnabled() )
        {
            getDigester().getLogger().debug( format( "[ObjectCreateRule]{%s} Pop '%s'",
                                                     getDigester().getMatch(),
                                                     top.getClass().getName() ) );
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return format( "ObjectCreateRule[className=%s, attributeName=%s]", className, attributeName );
    }

}
