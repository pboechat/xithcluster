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
package org.xith3d.render.preprocessing;

import org.openmali.vecmath2.Tuple3f;
import org.xith3d.render.states.units.FogStateUnit;
import org.xith3d.render.states.units.LightingStateUnit;
import org.xith3d.scenegraph.Node;

/**
 * This is the RenderAtom responsible for occluder nodes.
 * 
 * @author Marvin Froehlich (aka Qudus)
 */
public class ShadowAtom extends RenderAtom< Node >
{
    public static final int STATE_TYPE = 2;
    
    /**
     * Get the center of the bounds for this node and return it in the specified
     * point. The bounds used are the virtual world bounds, not the local
     * bounds.
     * 
     * @param p
     */
    @Override
    public < Tup extends Tuple3f > Tup getPosition( Tup p )
    {
        //if ( getNode().getWorldBounds() != null )
        if ( getNode().getWorldBounds() != null )
            getNode().getWorldBounds().getCenter( p );
        else
            getNode().getWorldTransform().getTranslation( p );
        
        return ( p );
    }
    
    public final void updateLightsAndFogs()
    {
        ( (LightingStateUnit)getStateUnit( LightingStateUnit.STATE_TYPE ) ).update();
        ( (FogStateUnit)getStateUnit( FogStateUnit.STATE_TYPE ) ).update();
    }
    
    protected ShadowAtom( int stateType, Node node )
    {
        super( stateType, node, true );
        
        /*
         * Important!
         * Initializes the state-types, which defines the order in which states
         * are applied to the GL, which is important.
         */
        org.xith3d.render.states.StateTypes.init();
        
        LightingStateUnit lsu = LightingStateUnit.makeLightingStateUnit( node.getInheritedNodeAttributes() );
        updateStateUnit( lsu );
        
        FogStateUnit fsu = FogStateUnit.makeFogStateUnit( node.getInheritedNodeAttributes() );
        updateStateUnit( fsu );
    }
    
    public ShadowAtom( Node node )
    {
        this( ShadowAtom.STATE_TYPE, node );
    }
}
