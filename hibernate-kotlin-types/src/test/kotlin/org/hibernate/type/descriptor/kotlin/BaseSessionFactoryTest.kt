package org.hibernate.type.descriptor.kotlin

import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.boot.MetadataSources
import org.hibernate.boot.registry.StandardServiceRegistry
import org.hibernate.boot.registry.StandardServiceRegistryBuilder
import org.hibernate.cfg.AvailableSettings
import org.hibernate.dialect.Dialect
import org.hibernate.dialect.H2Dialect
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance

/**
 * Base class for tests that require a Session to run.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
abstract class BaseSessionFactoryTest {
    protected open val dialectClass: Class<out Dialect> = H2Dialect::class.java
    protected open val jdbcDriver: String = "org.h2.Driver"
    protected open val jdbcUser: String? = "sa"

    protected abstract val jdbcUrl: String
    protected abstract val annotatedClasses: Array<Class<*>>

    private lateinit var serviceRegistry: StandardServiceRegistry
    private lateinit var sessionFactory: SessionFactory

    protected open fun beforeSessionFactoryCreated() = Unit

    protected open fun afterSessionFactoryClosed() = Unit

    @BeforeAll
    fun setUpSessionFactory() {
        beforeSessionFactoryCreated()

        val builder = StandardServiceRegistryBuilder()
            .applySetting(AvailableSettings.DIALECT, dialectClass.name)
            .applySetting(AvailableSettings.JAKARTA_JDBC_DRIVER, jdbcDriver)
            .applySetting(AvailableSettings.JAKARTA_JDBC_URL, jdbcUrl)
            .applySetting(AvailableSettings.HBM2DDL_AUTO, "create-drop")
        jdbcUser?.let { builder.applySetting(AvailableSettings.JAKARTA_JDBC_USER, it) }
        serviceRegistry = builder.build()

        sessionFactory = MetadataSources(serviceRegistry)
            .addAnnotatedClasses(*annotatedClasses)
            .buildMetadata()
            .buildSessionFactory()
    }

    @AfterAll
    fun tearDownSessionFactory() {
        try {
            if (::sessionFactory.isInitialized) sessionFactory.close()
            if (::serviceRegistry.isInitialized) StandardServiceRegistryBuilder.destroy(serviceRegistry)
        } finally {
            afterSessionFactoryClosed()
        }
    }

    protected fun <T : Any> persistAndReload(entity: T, type: Class<T>, id: (T) -> Any?): T =
        inTransaction { session ->
            session.persist(entity)
            session.flush()
            @Suppress("RedundantRequireNotNullCall")
            val identifier: Any = requireNotNull(id(entity))
            session.clear()
            requireNotNull(session.find(type, identifier))
        }

    protected fun <T> inTransaction(action: (Session) -> T): T =
        sessionFactory.openSession().use { session ->
            val transaction = session.beginTransaction()
            try {
                action(session).also { transaction.commit() }
            } catch (error: Throwable) {
                if (transaction.isActive) transaction.rollback()
                throw error
            }
        }
}
