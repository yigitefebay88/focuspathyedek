package com.focuspath.app.core.`data`.local

import androidx.room.InvalidationTracker
import androidx.room.RoomOpenDelegate
import androidx.room.migration.AutoMigrationSpec
import androidx.room.migration.Migration
import androidx.room.util.TableInfo
import androidx.room.util.TableInfo.Companion.read
import androidx.room.util.dropFtsSyncTriggers
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import javax.`annotation`.processing.Generated
import kotlin.Lazy
import kotlin.String
import kotlin.Suppress
import kotlin.collections.List
import kotlin.collections.Map
import kotlin.collections.MutableList
import kotlin.collections.MutableMap
import kotlin.collections.MutableSet
import kotlin.collections.Set
import kotlin.collections.mutableListOf
import kotlin.collections.mutableMapOf
import kotlin.collections.mutableSetOf
import kotlin.reflect.KClass

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class AppDatabase_Impl : AppDatabase() {
  private val _taskDao: Lazy<TaskDao> = lazy {
    TaskDao_Impl(this)
  }


  protected override fun createOpenDelegate(): RoomOpenDelegate {
    val _openDelegate: RoomOpenDelegate = object : RoomOpenDelegate(9,
        "e42a76baec04c43c3cce013047d138bb", "e41e8907eeb6c17a6604a814fbafde35") {
      public override fun createAllTables(connection: SQLiteConnection) {
        connection.execSQL("CREATE TABLE IF NOT EXISTS `tasks` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `notes` TEXT NOT NULL, `category` TEXT NOT NULL, `priority` INTEGER NOT NULL, `isCompleted` INTEGER NOT NULL, `dueDate` INTEGER NOT NULL, `rewardCoins` INTEGER NOT NULL, `energyLevel` INTEGER NOT NULL, `estimatedMinutes` INTEGER NOT NULL, `actualMinutes` INTEGER NOT NULL, `parentId` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `focus_history` (`date` TEXT NOT NULL, `totalFocusMinutes` INTEGER NOT NULL, `tasksCompleted` INTEGER NOT NULL, `sessionsCompleted` INTEGER NOT NULL, `sessionsInterrupted` INTEGER NOT NULL, `categoryDistribution` TEXT NOT NULL, PRIMARY KEY(`date`))")
        connection.execSQL("CREATE TABLE IF NOT EXISTS `habits` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `iconName` TEXT NOT NULL, `colorHex` TEXT NOT NULL, `streak` INTEGER NOT NULL, `longestStreak` INTEGER NOT NULL, `lastCompletedDate` INTEGER NOT NULL, `createdAt` INTEGER NOT NULL)")
        connection.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
        connection.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, 'e42a76baec04c43c3cce013047d138bb')")
      }

      public override fun dropAllTables(connection: SQLiteConnection) {
        connection.execSQL("DROP TABLE IF EXISTS `tasks`")
        connection.execSQL("DROP TABLE IF EXISTS `focus_history`")
        connection.execSQL("DROP TABLE IF EXISTS `habits`")
      }

      public override fun onCreate(connection: SQLiteConnection) {
      }

      public override fun onOpen(connection: SQLiteConnection) {
        internalInitInvalidationTracker(connection)
      }

      public override fun onPreMigrate(connection: SQLiteConnection) {
        dropFtsSyncTriggers(connection)
      }

      public override fun onPostMigrate(connection: SQLiteConnection) {
      }

      public override fun onValidateSchema(connection: SQLiteConnection):
          RoomOpenDelegate.ValidationResult {
        val _columnsTasks: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsTasks.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("notes", TableInfo.Column("notes", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("category", TableInfo.Column("category", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("priority", TableInfo.Column("priority", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("isCompleted", TableInfo.Column("isCompleted", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("dueDate", TableInfo.Column("dueDate", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("rewardCoins", TableInfo.Column("rewardCoins", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("energyLevel", TableInfo.Column("energyLevel", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("estimatedMinutes", TableInfo.Column("estimatedMinutes", "INTEGER", true,
            0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("actualMinutes", TableInfo.Column("actualMinutes", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsTasks.put("parentId", TableInfo.Column("parentId", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysTasks: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesTasks: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoTasks: TableInfo = TableInfo("tasks", _columnsTasks, _foreignKeysTasks,
            _indicesTasks)
        val _existingTasks: TableInfo = read(connection, "tasks")
        if (!_infoTasks.equals(_existingTasks)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |tasks(com.focuspath.app.core.data.local.TaskEntity).
              | Expected:
              |""".trimMargin() + _infoTasks + """
              |
              | Found:
              |""".trimMargin() + _existingTasks)
        }
        val _columnsFocusHistory: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsFocusHistory.put("date", TableInfo.Column("date", "TEXT", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("totalFocusMinutes", TableInfo.Column("totalFocusMinutes",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("tasksCompleted", TableInfo.Column("tasksCompleted", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("sessionsCompleted", TableInfo.Column("sessionsCompleted",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("sessionsInterrupted", TableInfo.Column("sessionsInterrupted",
            "INTEGER", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsFocusHistory.put("categoryDistribution", TableInfo.Column("categoryDistribution",
            "TEXT", true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysFocusHistory: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesFocusHistory: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoFocusHistory: TableInfo = TableInfo("focus_history", _columnsFocusHistory,
            _foreignKeysFocusHistory, _indicesFocusHistory)
        val _existingFocusHistory: TableInfo = read(connection, "focus_history")
        if (!_infoFocusHistory.equals(_existingFocusHistory)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |focus_history(com.focuspath.app.core.data.local.FocusHistoryEntity).
              | Expected:
              |""".trimMargin() + _infoFocusHistory + """
              |
              | Found:
              |""".trimMargin() + _existingFocusHistory)
        }
        val _columnsHabits: MutableMap<String, TableInfo.Column> = mutableMapOf()
        _columnsHabits.put("id", TableInfo.Column("id", "INTEGER", true, 1, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("title", TableInfo.Column("title", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("description", TableInfo.Column("description", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("iconName", TableInfo.Column("iconName", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("colorHex", TableInfo.Column("colorHex", "TEXT", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("streak", TableInfo.Column("streak", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("longestStreak", TableInfo.Column("longestStreak", "INTEGER", true, 0,
            null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("lastCompletedDate", TableInfo.Column("lastCompletedDate", "INTEGER",
            true, 0, null, TableInfo.CREATED_FROM_ENTITY))
        _columnsHabits.put("createdAt", TableInfo.Column("createdAt", "INTEGER", true, 0, null,
            TableInfo.CREATED_FROM_ENTITY))
        val _foreignKeysHabits: MutableSet<TableInfo.ForeignKey> = mutableSetOf()
        val _indicesHabits: MutableSet<TableInfo.Index> = mutableSetOf()
        val _infoHabits: TableInfo = TableInfo("habits", _columnsHabits, _foreignKeysHabits,
            _indicesHabits)
        val _existingHabits: TableInfo = read(connection, "habits")
        if (!_infoHabits.equals(_existingHabits)) {
          return RoomOpenDelegate.ValidationResult(false, """
              |habits(com.focuspath.app.core.data.local.HabitEntity).
              | Expected:
              |""".trimMargin() + _infoHabits + """
              |
              | Found:
              |""".trimMargin() + _existingHabits)
        }
        return RoomOpenDelegate.ValidationResult(true, null)
      }
    }
    return _openDelegate
  }

  protected override fun createInvalidationTracker(): InvalidationTracker {
    val _shadowTablesMap: MutableMap<String, String> = mutableMapOf()
    val _viewTables: MutableMap<String, Set<String>> = mutableMapOf()
    return InvalidationTracker(this, _shadowTablesMap, _viewTables, "tasks", "focus_history",
        "habits")
  }

  public override fun clearAllTables() {
    super.performClear(false, "tasks", "focus_history", "habits")
  }

  protected override fun getRequiredTypeConverterClasses(): Map<KClass<*>, List<KClass<*>>> {
    val _typeConvertersMap: MutableMap<KClass<*>, List<KClass<*>>> = mutableMapOf()
    _typeConvertersMap.put(TaskDao::class, TaskDao_Impl.getRequiredConverters())
    return _typeConvertersMap
  }

  public override fun getRequiredAutoMigrationSpecClasses(): Set<KClass<out AutoMigrationSpec>> {
    val _autoMigrationSpecsSet: MutableSet<KClass<out AutoMigrationSpec>> = mutableSetOf()
    return _autoMigrationSpecsSet
  }

  public override
      fun createAutoMigrations(autoMigrationSpecs: Map<KClass<out AutoMigrationSpec>, AutoMigrationSpec>):
      List<Migration> {
    val _autoMigrations: MutableList<Migration> = mutableListOf()
    return _autoMigrations
  }

  public override fun taskDaoProvider(): TaskDao = _taskDao.value
}
