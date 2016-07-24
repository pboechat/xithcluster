xithcluster
================

**xithcluster** is an extention to the Java scene graph library [Xith3D](http://xith.org/).

It's a complete solution to distribute rendering between multiple computers using a method called Sort-Last.

Developed as my final graduation project.

Published in the 8th Brazilian Virtual and Augmented Reality Workshop (WRVA). You can download it [here](http://pedroboechat.com/publications/XithCluster%20-%20Uma%20Biblioteca%20de%20Grafo%20de%20Cena%20Distribu%C3%ADdo.pdf) (available only in Portuguese, unfortunately).

----------

### Install & Run

#### Single machine test:

1. Extract each *zip* file found in dist to a directory of the same name.
2. Copy *lib* to each one of the other directories.
3. Execute first *sampleApp/run.bat[.sh]*, then *composerApp/run.bat[.sh]* and *rendererApp/run.bat[.sh]*.

#### Cluster test:

On the master cluster node do:

1. Extract sampleApp.zip to a directory.
2. Open *xithcluster.properties* and adjust *listening.address* appropriately (this will later be referred as *master.listening.address*)
3. Execute *run.bat[.sh]*

On the slave cluster node used as composer do:

1. Extract composerApp.zip to a directory.
2. Open *composerApp.properties* and: 
	2.1. Adjust *master.listening.address* accordingly (see master cluster configuration)
	2.2. Adjust *renderers.connection.[address|port]* and to the interface/port you expect the rendering cluster nodes to connect.
3. [Optional] Select your custom composition strategy with *composition.strategy.classname*

On the slave cluster nodes used as renderers do:

1. Extract rendererApp.zip to a directory.
2. Open *rendererApp.properties* and adjust *master.listening.address* and *composer.listening.address* accordingly (see master node and composer node configurations).
3. [Optional] Define composition precedence for transparency resolution with *composition.order*.
4. Execute *run.bat[.sh]*.

----------

### Getting Started

 1. Create a class that extends *br.edu.univercidade.cc.xithcluster.SampleApplication*. 
 2. Override *public BranchNode createSceneRoot(Animator animator)* and create a scene as you would in regular Xith3D. 
 3. Add animatable objects to the given animator so that animation can play synchronously.
 4. For better performance, use primitive provided (ie.: *br.edu.univercidade.cc.xithcluster.nodes.primitives.Cube*)

		package br.edu.univercidade.cc.xithcluster.samples;

		import org.jagatoo.input.devices.components.Key;
		import org.jagatoo.input.events.KeyPressedEvent;
		import org.openmali.vecmath2.Colorf;
		import org.openmali.vecmath2.Tuple3f;
		import org.openmali.vecmath2.Vector3f;
		import org.xith3d.loop.opscheduler.Animator;
		import org.xith3d.scenegraph.BranchGroup;
		import org.xith3d.scenegraph.Group;
		import org.xith3d.scenegraph.Material;
		import org.xith3d.schedops.movement.AnimatableGroup;
		import org.xith3d.schedops.movement.GroupRotator;
		import org.xith3d.schedops.movement.TransformationDirectives;
		import br.edu.univercidade.cc.xithcluster.SampleApplication;
		import br.edu.univercidade.cc.xithcluster.nodes.primitives.Sphere;
		import br.edu.univercidade.cc.xithcluster.utils.SceneUtils;

		public class EarthAndMoonSample extends SampleApplication {
			
			public EarthAndMoonSample(Tuple3f eyePosition, Tuple3f viewFocus) {
				super(eyePosition, viewFocus);
			}

			@Override
			protected String getJARName() {
				return "solarSystemSample.jar";
			}
			
			@Override
			protected void keyPressed(KeyPressedEvent event, Key key) {
			}

			@Override
			protected BranchGroup createSceneRoot(Animator animator) {
				BranchGroup root;
				Group group;
				AnimatableGroup earthGroup, moonGroup;
				GroupRotator earthRotation, moonRotation;
				
				root = new BranchGroup();
				
				group = new Group();
				group.setName("allGeometries");
				root.addChild(group);
				
				earthRotation = new GroupRotator(new TransformationDirectives(new Vector3f(0.0f, 1.0f, 0.0f), 0.0f, 0.1f));
				earthGroup = new AnimatableGroup(earthRotation);
				group.addChild(earthGroup);
				
				animator.addAnimatableObject(earthGroup);

				// The earth
				
				Sphere earth = SceneUtils.addSphere(earthGroup, 
													"earth", 
													4.0f,
													100,
													100,
													new Tuple3f(0.0f, 0.0f, 0.0f), 
													SceneUtils.loadTexture2D("resources/textures/earth.png"));

				Colorf emissiveColor = new Colorf(1.0f, 1.0f, 1.0f); 
				Colorf specularColor = new Colorf(1.0f, 1.0f, 1.0f);
				Material material = new Material(true, 1.0f);
				material.setEmissiveColor(emissiveColor);
				material.setSpecularColor(specularColor);
				earth.getAppearance().setMaterial(material);

				moonRotation = new GroupRotator(new TransformationDirectives(new Vector3f(0.0f, 1.0f, 0.0f), 0.0f, 0.05f));
				moonGroup = new AnimatableGroup(moonRotation);
				group.addChild(moonGroup);
				
				animator.addAnimatableObject(moonGroup);
				
				// The moon
				
				Sphere moon = SceneUtils.addSphere(moonGroup, 
													"moon", 
													0.5f,
													30,
													30,
													new Tuple3f(4.5f, 0.0f, 0.0f), 
													SceneUtils.loadTexture2D("resources/textures/moon.png"));
				
				moon.getAppearance().setMaterial(material);
				
				// Light of the sun
				
				SceneUtils.addSpotLight(root, 
						"lightOfTheSun", 
						new Colorf(0.5f, 0.5f, 0.5f), 
						new Tuple3f(-50.0f, 0.0f, 0.0f), 
						new Tuple3f(1.0f, 0.0f, 0.0f), 
						new Tuple3f(0f, 0f, 0f), 
						60.0f, 
						1.0f);
				
				return root;
			}
			
			/*
			 * ================ 
			 * 		MAIN 
			 * ================
			 */
			public static void main(String[] args) {
				EarthAndMoonSample earthAndMoonSample = new EarthAndMoonSample(new Tuple3f(0.0f, 0.0f, 10.0f), new Tuple3f(0.0f, 0.0f, 0.0f));
				earthAndMoonSample.init(args);
			}
			
		}




The BranchNode returned by the *createSceneRoot(..)* method will be dynamically distributed among available renderers.


----------


### Disclaimer

xithcluster was only tested with Xith3D version 0.9.7.
