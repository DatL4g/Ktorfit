package de.jensklingenberg.ktorfit.requestData


import de.jensklingenberg.ktorfit.model.ParameterData
import de.jensklingenberg.ktorfit.model.annotations.Field
import de.jensklingenberg.ktorfit.model.annotations.FieldMap
import de.jensklingenberg.ktorfit.utils.surroundIfNotEmpty
import de.jensklingenberg.ktorfit.utils.surroundWith

/**
 * Source for the "fields" argument of [de.jensklingenberg.ktorfit.RequestData]
 */

fun getFieldArgumentsText(params: List<ParameterData>): String {
    //Get all Parameter with @Field and add them to a map

    val fieldDataStringList = mutableListOf<String>()

    val fieldDataList = params.filter { it.hasAnnotation<Field>() }.map { parameterData ->
        val query = parameterData.annotations.filterIsInstance<Field>().first()
        val encoded = query.encoded
        val data = parameterData.name
        val queryKey = query.value.surroundWith("\"")
        val type = "FieldType.FIELD"

        "FieldData($queryKey,$data,$encoded,$type)"
    }

    fieldDataStringList.addAll(fieldDataList)

    val fieldMapStrings = params.filter { it.hasAnnotation<FieldMap>() }.map { parameterData ->
        val queryMap = parameterData.findAnnotationOrNull<FieldMap>()!!
        val encoded = queryMap.encoded
        val data = parameterData.name
        val keyName = "\"\""
        val type = "FieldType.FIELDMAP"

        "FieldData($keyName,$data,$encoded,$type)"
    }

    fieldDataStringList.addAll(fieldMapStrings)

    return fieldDataStringList.joinToString { it }.surroundIfNotEmpty("fields = listOf(", ")")
}