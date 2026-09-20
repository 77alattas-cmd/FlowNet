package com.fn.has.code.data.local.db.dao

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fn.has.code.data.local.db.AppDatabase
import com.fn.has.code.data.local.db.entity.BlockedDomainEntity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BlockedDomainDaoTest {

    private lateinit var database: AppDatabase
    private lateinit var dao: BlockedDomainDao

    @Before
    fun setup() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).allowMainThreadQueries().build()

        dao = database.blockedDomainDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fn insertAndGetActiveBlockedDomains() = runBlocking {
        val domain = BlockedDomainEntity(domain = "badsite.com", category = "CUSTOM", isEnabled = true)
        dao.insertDomain(domain)

        val activeList = dao.getActiveBlockedDomains()
        assertEquals(1, activeList.size)
        assertEquals("badsite.com", activeList[0].domain)
    }

    @Test
    fn deleteByDomainName_removesDomainSuccessfully() = runBlocking {
        val domain = BlockedDomainEntity(domain = "malware.net", category = "MALWARE", isEnabled = true)
        dao.insertDomain(domain)

        dao.deleteByDomainName("malware.net")
        val activeList = dao.getActiveBlockedDomains()

        assertTrue(activeList.isEmpty())
    }
}
