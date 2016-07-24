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
package org.xith3d.render.preprocessing.sorting;

import org.openmali.vecmath2.Point3f;
import org.xith3d.render.preprocessing.RenderAtom;
import org.xith3d.render.preprocessing.RenderBin;
import org.xith3d.render.preprocessing.RenderBin.DynamicAtomArray;
import org.xith3d.scenegraph.Transform3D;
import org.xith3d.utility.comparator.Sorter;

/**
 * This sorter sorts RenderAtoms back-to-front.
 * 
 * @author David Yazel
 * @author Marvin Froehlich (aka Qudus)
 */
public class BackToFrontByBoundingSphereAndEyeRayIntersectionRenderBinSorter extends BackToFrontRenderBinSorter
{
    private Point3f viewPosition = new Point3f();
    private Point3f tmpPos = new Point3f();
    
    /**
     * {@inheritDoc}
     */
    @Override
    public void sortRenderBin( RenderBin renderBin, Transform3D viewTransform )
    {
        viewTransform.getTranslation( viewPosition );
        final DynamicAtomArray atoms = renderBin.getAtoms();
        
        for ( int i = 0; i < atoms.size(); i++ )
        {
            final RenderAtom< ? > atom = atoms.get( i );
            
            atom.getPosition( tmpPos );
            atom.setCompareIndicators( tmpPos.distanceSquared( viewPosition ) - atom.getNode().getWorldBounds().getMaxCenterDistanceSquared(), tmpPos.getZ(), null );
        }
        
        //Arrays.sort( buckets.getRawArray(), 0, buckets.size(), this );
        Sorter.quickSort( atoms.getRawArray(), 0, atoms.size() - 1, this );
        //head = Sorter.mergeSort( head, this );
    }
    
    public BackToFrontByBoundingSphereAndEyeRayIntersectionRenderBinSorter()
    {
    }
}
