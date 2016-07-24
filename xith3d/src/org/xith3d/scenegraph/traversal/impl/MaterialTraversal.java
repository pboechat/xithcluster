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
package org.xith3d.scenegraph.traversal.impl;

import org.xith3d.scenegraph.Appearance;
import org.xith3d.scenegraph.Material;
import org.xith3d.scenegraph.Node;
import org.xith3d.scenegraph.GroupNode;
import org.xith3d.scenegraph.Shape3D;
import org.xith3d.scenegraph.traversal.TraversalCallback;

/**
 * This class traverses a subgroup of the scenegraph and
 * applies a Material to each found Shape3D.
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class MaterialTraversal implements TraversalCallback
{
    private Material mat;
    private Shape3D shape;
    
    public boolean traversalOperation( Node node )
    {
        if ( node instanceof Shape3D )
        {
            shape = (Shape3D)node;
            if ( shape.getAppearance() == null )
                shape.setAppearance( new Appearance() );
            
            shape.getAppearance().setMaterial( mat );
        }
        
        return ( true );
    }
    
    public boolean traversalCheckGroup( GroupNode group )
    {
        return ( true );
    }
    
    public MaterialTraversal( Material mat )
    {
        this.mat = mat;
    }
}
