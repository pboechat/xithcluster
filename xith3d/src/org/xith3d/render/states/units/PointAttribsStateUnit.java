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
package org.xith3d.render.states.units;

import org.xith3d.render.states.StateMap;
import org.xith3d.render.states.StateUnit;
import org.xith3d.scenegraph.PointAttributes;

/**
 * The point attributes shader encapsulates the Java3d standard PointAttributes
 * which are attached to an appearance.
 * 
 * @author Yuri Vl. Gushchin
 * @author Marvin Froehlich (aka Qudus)
 */
public class PointAttribsStateUnit extends StateUnit
{
    public static final int STATE_TYPE = StateMap.newStateType();
    
    private static final StateMap POINT_STATE_MAP = new StateMap();
    
    private static final PointAttribsStateUnit DEFAULT_POINT = new PointAttribsStateUnit( new PointAttributes(), true );
    
    private PointAttributes pointAttribs;
    
    public final PointAttributes getPointAttibutes()
    {
        return ( pointAttribs );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public PointAttributes getNodeComponent()
    {
        return ( pointAttribs );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean isTranslucent()
    {
        if ( pointAttribs == null )
            return ( false );
        if ( !pointAttribs.isSortEnabled() )
            return ( false );
        
        return ( pointAttribs.isPointAntialiasingEnabled() );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public final long getStateId()
    {
        return ( pointAttribs.getStateId() );
    }
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return ( this.getClass().getSimpleName() + "( state-type: " + getStateType() + ", " + "state-ID: " + getStateId() + " )" );
    }
    
    public final void update( PointAttributes pa )
    {
        this.pointAttribs = pa;
        
        POINT_STATE_MAP.assignState( pa );
    }
    
    private PointAttribsStateUnit( PointAttributes pa, boolean isDefault )
    {
        super( STATE_TYPE, isDefault );
        
        update( pa );
    }
    
    public static PointAttribsStateUnit makePointAttribsStateUnit( PointAttributes pa, StateUnit[] cache )
    {
        if ( pa == null )
        {
            return ( DEFAULT_POINT );
        }
        else if ( cache[ STATE_TYPE ] != null )
        {
            final PointAttribsStateUnit stateUnit = ( (PointAttribsStateUnit)cache[ STATE_TYPE ] );
            stateUnit.update( pa );
            
            return ( stateUnit );
        }
        else
        {
            PointAttribsStateUnit stateUnit = new PointAttribsStateUnit( pa, false );
            cache[ STATE_TYPE ] = stateUnit;
            
            return ( stateUnit );
        }
    }
}
