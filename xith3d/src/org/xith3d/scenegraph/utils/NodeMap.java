/**
 * Copyright (c) 2003-2010, Xith3D Project Group all rights reserved.
 *
 * Portions based on the Java3D interface, Copyright by Sun Microsystems.
 * Many thanks to the developers of Java3D and Sun Microsystems for their
 * innovation and design.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the 'Xith3D Project Group' nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) A
 * RISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE
 */
package org.xith3d.scenegraph.utils;

import org.xith3d.scenegraph.Node;
import org.xith3d.scenegraph.GroupNode;

/**
 * A Node Map.<br>
 * <br>
 * Useful in picking, for example :<br>
 * <code>NodeMap<OBJModel> map = new NodeMap("obj");</code><br>
 * Each time you create a pickable OBJ Model :<br>
 * <code>map.prepare(model);</code><br>
 * You have a PickResult, want your OBJ model back</br>
 * <code>OBJModel model = map.get(pickResult.getNode());<br>
 * Then you can do whatever you want with it !<br>
 * 
 * @author Amos Wenger (aka BlueSky)
 */
public class NodeMap<T extends GroupNode> {

    String id;
    
    /**
     * Create a new NodeMap
     * @param id The ID which will be used when storing
     * the data in Nodes. If you have several NodeMap(s)
     * on same objects, have different IDs
     */
    public NodeMap(String id) {
        
        this.id = id;
        
    }
    
    public void prepare(T group) {
        prepare(group, group);
    }
    
    @SuppressWarnings("unchecked")
    public void prepare(T group, Object object) {
        final int numChildren = group.numChildren();
        for (int i = 0; i < numChildren; i++) {
            final Node node = group.getChild(i);
            
            node.setUserData(id, object);
            if(node instanceof GroupNode) {
                prepare((T)node, object);
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    public T get(Node node) {
        return (T) node.getUserData(id);
    }
    
}
