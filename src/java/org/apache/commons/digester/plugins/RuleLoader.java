/* $Id: RuleLoader.java,v 1.5 2004/05/10 06:36:38 skitching Exp $
 *
 * Copyright 2004 The Apache Software Foundation.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 
 
package org.apache.commons.digester.plugins;

import org.apache.commons.digester.Digester;

/**
 * Interface for classes which can dynamically load custom
 * plugin rules associated with a user's plugin class.
 * <p>
 * Each plugin declaration has an associated RuleLoader instance, and that
 * instance's addRules method is invoked each time the input xml specifies
 * that an instance of that plugged-in class is to be created.
 * <p>
 * This is an abstract class rather than an interface in order to make
 * it possible to enhance this class in future without breaking binary
 * compatibility; it is possible to add methods to an abstract class, but
 * not to an interface. 
 *
 * @since 1.6
 */

public abstract class RuleLoader {
    
    /**
     * Configures the digester with custom rules for some plugged-in
     * class.
     * <p>
     * This method is invoked when the start of an xml tag is encountered
     * which maps to a PluginCreateRule. Any rules added here are removed
     * from the digester when the end of that xml tag is encountered.
     */
    public abstract void addRules(Digester d, String path) throws PluginException;
}
