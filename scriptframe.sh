#!/bin/sh
frame_dir="G:\S4\MrNaina\work\framework\sprint0"

cd "$frame_dir" || exit

javac -d . *.java

jar cf FrontController.jar  mg
jar cf AnnotationController.jar  mg

mv FrontController.jar "G:/S4/MrNaina/work/test/lib"
mv AnnotationController.jar "G:/S4/MrNaina/work/test/lib"
sleep 60

