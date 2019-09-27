
BOM Helper Maven Plugin aims to help BOM creators by providing goals that allow to verify, modify and generate BOMs  

### What is BOM?

BOM stands for [Bill Of Materials](https://en.wikipedia.org/wiki/Bill_of_materials) and is used to describe 
the list list of the raw materials needed to manufacture an end product. In the context of Maven, a BOM is a special kind of project that contains a list of the specific versions of all dependencies that a project may use. It allows developers to add dependency to their projects/module without worrying about the version. For more details about Maven BOM please see [Dependency Management section of Maven's Dependency Mechanism](https://maven.apache.org/guides/introduction/introduction-to-dependency-mechanism.html#Dependency_Management) and [Spring with Maven BOM](https://www.baeldung.com/spring-maven-bom) article.


### How this plug-in can help ?

There are number of different ways to create/generate Maven BOMs. Many may already have own ways to solve the issues this plug-in trues to solve. But for those who don't have such tool, this plug-in can help with 

 - making sure the BOM entries are resolvable. The [`resolve` goal](/bom-helper-maven-plugin/resolve-mojo.html) will iterate over the list and fail the build if the BOM contains unresolvable dependency.
 - sorting the dependencies alphabetically to make it easy for humans to read the BOM. See [`sort` goal](/bom-helper-maven-plugin/sort-mojo.html) for details  
 - generating (or updating existing) BOM from local jar files containing Maven metadata. See [`fromJars` goal](/bom-helper-maven-plugin/fromJars-mojo.html) for details  

### License

The project is released under [Apache License, Version 2.0](https://www.apache.org/licenses/LICENSE-2.0)
