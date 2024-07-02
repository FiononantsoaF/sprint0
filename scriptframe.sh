#!/bin/sh

# Define directories
frame_dir="G:\S4\MrNaina\work\framework\sprint0"
lib_dir="G:\S4\MrNaina\work\framework\sprint0\lib"
test_lib_dir="G:\S4\MrNaina\work\testFramework\test\lib"

cd "$frame_dir" || exit

# Compile Java files with necessary libraries in the classpath
javac -d "$frame_dir" -cp "$lib_dir/*" "$frame_dir"/*.java

for jar_file in FrontController AnnotationController GetAnnotation ModelView Post Param AnnotationClass AnnotationAttribut; do
    jar cf "${jar_file}.jar" mg
    mv "${jar_file}.jar" "$test_lib_dir"
done

# Wait for 60 seconds
sleep 60
