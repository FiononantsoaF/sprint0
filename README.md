# sprint0
Creer une classe .java  dans votre espace de travail 
Mettre une package de votre choix
Annoter votre classe (exemple: @AnnotationController)
Annoter la methode de votre classe annoter controller (exemple: @GetController)
nommer init-param comme "controller-package"
declarer dans  param-value  votre package 
exemple :
    <servlet>
        <servlet-name>FrontController</servlet-name>
        <servlet-class>mg.itu.prom16.FrontController</servlet-class>
        <init-param>
            <param-name>controller-package</param-name>
            <param-value><!-- ecrire votre package ici --></param-value>
        </init-param>
    </servlet>
    <servlet-mapping>
        <servlet-name>FrontController</servlet-name>
        <url-pattern>/*</url-pattern>
    </servlet-mapping>



