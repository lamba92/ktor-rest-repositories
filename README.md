# Ktor Rest Repositories Feature

This project is an attempt to easily build routes for Entities declared using [Ktorm](https://github.com/vincentlauvlwj/Ktorm) (hoping one day to support Exposed as well, [but no serialization so far](https://github.com/JetBrains/Exposed/issues/497)).

**NB**: at the moment **nothing is working** and I'm doing this in my free time! Help would be much appreciated :P

# Usage

### Basics
The usage is pretty straight forward. Declare your Ktorm  entities as such:
```kotlin
interface StringIdEntity : Entity<StringIdEntity> {
    val id: String
    var value1: String
    var value2: Int
}

object StringIdEntities : Table<StringIdEntity>("stringIdEntities") {
    val id by varchar("id").primaryKey().bindTo { it.id }
    val value1 by varchar("value1").bindTo { it.value1 }
    val value2 by int("value2").bindTo { it.value2 }
}
```
Then install the `RestRepositories` feature and register the entity:
```kotlin
fun Application.myModule() {
    val db: Database = getDatabase()
    
    // Won't work without ContentNegotiation setted up!
    install(ContentNegotiation) {
        restRepositories()
    }

    install(RestRepositories) {
        repositoryPath = "mRepos"
        registerEntity<StringIdEntity, String>(StringIdEntities, db, SERIALIZABLE) {
            entityPath = "strings"
            addEndpoint(Get)
        }
    }
}
```

This code will create 2 HTTP `Get` endpoints:
 - `mRepos/strings/{entityId}`: returns the item with primary-key=`entityId`.
 - `mRepos/strings` with a list of IDs in the body: returns a list containing the items.
 
A the moment the implemented HTTP methods are `Get`, `Post`, `Put` (disabled at runtime due to bug), `Delete`. Each method when registered will generate 2 endpoint as the example above. Every method, except `Delete` which return an empty 200 code, will return the entire item just modified.

### Authentication support

`RestRepositories` supports Ktor [Authentication](https://ktor.io/servers/features/authentication.html) Feature. To enable it, specify that the endpoint is authenticated and the name of the authentication provider:
 ```kotlin
registerEntity<StringIdEntity, String>(StringIdEntities, db, SERIALIZABLE) {
    addEndpoint(Get) {
        isAuthenticated = true
        authName = "myAuth" // if not set, the default auth will be used 
    }
}
```

### Setting constraints on entity updates
It is possible to specify constraints that the data of your entity must follow in order to allow read/write from database:
 ```kotlin
registerEntity<StringIdEntity, String>(StringIdEntities, db, SERIALIZABLE) {
    addEndpoint(Get) {
        isAuthenticated = true
        restRepositoryInterceptor = { mEntity: StringIdEntity -> // this: PipelineContext<Unit, ApplicationCall>
            assert(entity.value1 == call.principal<UserIdPrincipal>()!!.name) { "value1 != userId" }        
            entity
        }
    }
}
```
The entity eventually modified must be returned. When setting `restRepositoryInterceptor` inside `Post` or `Put`, the entity returned is the one that will be written on the DB. 

### Gradle

