package de.jensklingenberg.ktorfit.generator

import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.Dependencies
import com.squareup.kotlinpoet.*
import de.jensklingenberg.ktorfit.findAnnotationOrNull
import de.jensklingenberg.ktorfit.model.MyClass
import de.jensklingenberg.ktorfit.model.annotations.Streaming
import de.jensklingenberg.ktorfit.requestData.*
import de.jensklingenberg.ktorfit.resolveTypeName
import java.io.OutputStreamWriter


fun generateClassImpl(myClasses: List<MyClass>, codeGenerator: CodeGenerator) {
    myClasses.forEach { myClass ->
        val file = getFileSpec(myClass).toString().replace("WILDCARDIMPORT", "*")

        val packageName = myClass.packageName
        val className = myClass.name

        codeGenerator.createNewFile(Dependencies.ALL_FILES, packageName, "_${className}Impl", "kt").use { output ->
            OutputStreamWriter(output).use { writer ->
                writer.write(file)
            }
        }
    }
}




fun getFileSpec(myClass: MyClass): FileSpec {
    val funcs: List<FunSpec> = getFunSpecs(myClass)

    val properties = myClass.properties.map { property ->
        val propBuilder = PropertySpec.builder(
            property.simpleName.asString(),
            TypeVariableName(property.type.resolve().resolveTypeName())
        )
            .addModifiers(KModifier.OVERRIDE)
            .mutable(property.isMutable)
            .getter(
                FunSpec.getterBuilder()
                    .addStatement("TODO(\"Not yet implemented\")")
                    .build()
            )

        if (property.isMutable) {
            propBuilder.setter(
                FunSpec.setterBuilder()
                    .addParameter("value", TypeVariableName(property.type.resolve().resolveTypeName()))
                    .build()
            )
        } else {
        }

        propBuilder.build()
    }

    return FileSpec.builder(myClass.packageName, "_${myClass.name}Impl")
        .addFileComment("Generated by Ktorfit")
        .addImports(myClass.imports)
        .addImport("de.jensklingenberg.ktorfit.internal", "KtorfitClient")
        .addType(
            TypeSpec.classBuilder("_${myClass.name}Impl")
                .addSuperinterface(ClassName(myClass.packageName, myClass.name))
                .addKtorfitSuperInterface(myClass.superClasses)
                .primaryConstructor(
                    FunSpec.constructorBuilder()
                        .addParameter("client", TypeVariableName("KtorfitClient"))
                        .build()
                )
                .addProperty(
                    PropertySpec.builder("client", TypeVariableName("KtorfitClient"))
                        .addModifiers(KModifier.PRIVATE)
                        .initializer("client")
                        .build()
                )
                .addFunctions(funcs)
                .addProperties(properties)
                .build()
        )

        .build()
}


fun getFunSpecs(myClass: MyClass): List<FunSpec> = myClass.functions.map { myFunc ->

    val requestDataArgumentsText = RequestDataArgumentNode(
        myFunc,
    ).toString()

    val returnTypeName = myFunc.returnType.name
    val typeWithoutOuterType = returnTypeName.substringAfter("<").substringBeforeLast(">")

    FunSpec.builder(myFunc.name)
        .addModifiers(mutableListOf(KModifier.OVERRIDE).also {
            if (myFunc.isSuspend) {
                it.add(KModifier.SUSPEND)
            }
        })
        .returns(TypeVariableName(myFunc.returnType.name))
        .addParameters(myFunc.params.map {
            ParameterSpec(it.name, TypeVariableName(it.type.name))
        })
        .addStatement(requestDataArgumentsText)
        .addStatement(
            if (myFunc.isSuspend) {
                "return client.suspendRequest<${returnTypeName}, $typeWithoutOuterType>(requestData)"
            } else {
                "return client.request<${returnTypeName}, $typeWithoutOuterType>(requestData)"
            }
        )
        .build()
}

/**
 * Support for extending multiple interfaces, is done with Kotlin delegation. Ktorfit interfaces can only extend other Ktorfit interfaces, so there will
 * be a generated implementation for each interface that we can use.
 */
fun TypeSpec.Builder.addKtorfitSuperInterface(superClasses: List<String>): TypeSpec.Builder {
    superClasses.forEach { superClassQualifiedName ->
        val superTypeClassName = superClassQualifiedName.substringAfterLast(".")
        val superTypePackage = superClassQualifiedName.substringBeforeLast(".")
        this.addSuperinterface(
            ClassName(superTypePackage, superTypeClassName),
            CodeBlock.of("${superTypePackage}._${superTypeClassName}Impl(client)")
        )
    }

    return this
}


private fun FileSpec.Builder.addImports(imports: List<String>): FileSpec.Builder {

    imports.forEach {
        /**
         * Wildcard imports are not allowed by KotlinPoet, as a work around * is replaced with WILDCARDIMPORT and it will be replaced again
         * after Kotlin Poet generated the source code
         */
        val className = it.substringAfterLast(".").replace("*", "WILDCARDIMPORT")
        val packageName = it.substringBeforeLast(".")
        this.addImport(packageName, className)
    }
    return this
}