package com.github.lamba92.ktor.features.test.tests

interface RestRepositoriesTest {

    val restRepoPath: String
    val entitiesPath: String

    val urlPath
        get() = "$restRepoPath/$entitiesPath"

    fun `single item GET test`()
    fun `multiple item GET test`()

    fun `single item PUT test`()
    fun `multiple item PUT test`()

    fun `single item POST test`()
    fun `multiple item POST test`()

    fun `single item DELETE test`()
    fun `multiple item DELETE test`()

}
