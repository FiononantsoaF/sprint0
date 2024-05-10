#!/bin/sh
frame_dir="G:/S4/MrNaina/work/framework/Framework"

cd "$frame_dir" || exit

javac -d . *.java

jar cf FrontController.jar  mg
mv FrontController.jar "G:/S4/MrNaina/work/test/lib"
sleep 60

