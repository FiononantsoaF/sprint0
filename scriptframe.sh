#!/bin/sh
frame_dir="G:\S4\MrNaina\work\framework\sprint0"

cd "$frame_dir" || exit

javac -d . *.java

jar cf FrontController.jar  mg
jar cf AnnotationController.jar  mg
jar cf GetAnnotation.jar  mg
jar cf ModelView.jar  mg
jar cf Post.jar  mg
jar cf Param.jar  mg

mv FrontController.jar "G:\S4\MrNaina\work\testFramework\test\lib"
mv AnnotationController.jar "G:\S4\MrNaina\work\testFramework\test\lib"
mv GetAnnotation.jar "G:\S4\MrNaina\work\testFramework\test\lib"
mv ModelView.jar "G:\S4\MrNaina\work\testFramework\test\lib"
mv Post.jar "G:\S4\MrNaina\work\testFramework\test\lib"
mv Param.jar "G:\S4\MrNaina\work\testFramework\test\lib"
sleep 60

