package com.focuspath.app.core.`data`.local

import androidx.room.EntityDeleteOrUpdateAdapter
import androidx.room.EntityInsertAdapter
import androidx.room.RoomDatabase
import androidx.room.coroutines.createFlow
import androidx.room.util.getColumnIndexOrThrow
import androidx.room.util.getTotalChangedRows
import androidx.room.util.performSuspending
import androidx.sqlite.SQLiteStatement
import javax.`annotation`.processing.Generated
import kotlin.Boolean
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.Suppress
import kotlin.Unit
import kotlin.collections.List
import kotlin.collections.MutableList
import kotlin.collections.mutableListOf
import kotlin.reflect.KClass
import kotlinx.coroutines.flow.Flow

@Generated(value = ["androidx.room.RoomProcessor"])
@Suppress(names = ["UNCHECKED_CAST", "DEPRECATION", "REDUNDANT_PROJECTION", "REMOVAL"])
public class TaskDao_Impl(
  __db: RoomDatabase,
) : TaskDao {
  private val __db: RoomDatabase

  private val __insertAdapterOfTaskEntity: EntityInsertAdapter<TaskEntity>

  private val __insertAdapterOfFocusHistoryEntity: EntityInsertAdapter<FocusHistoryEntity>

  private val __insertAdapterOfHabitEntity: EntityInsertAdapter<HabitEntity>

  private val __deleteAdapterOfTaskEntity: EntityDeleteOrUpdateAdapter<TaskEntity>

  private val __deleteAdapterOfHabitEntity: EntityDeleteOrUpdateAdapter<HabitEntity>

  private val __updateAdapterOfTaskEntity: EntityDeleteOrUpdateAdapter<TaskEntity>

  private val __updateAdapterOfHabitEntity: EntityDeleteOrUpdateAdapter<HabitEntity>
  init {
    this.__db = __db
    this.__insertAdapterOfTaskEntity = object : EntityInsertAdapter<TaskEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `tasks` (`id`,`title`,`notes`,`category`,`priority`,`isCompleted`,`dueDate`,`rewardCoins`,`energyLevel`,`estimatedMinutes`,`actualMinutes`,`parentId`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.notes)
        statement.bindText(4, entity.category)
        statement.bindLong(5, entity.priority.toLong())
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.dueDate)
        statement.bindLong(8, entity.rewardCoins.toLong())
        statement.bindLong(9, entity.energyLevel.toLong())
        statement.bindLong(10, entity.estimatedMinutes.toLong())
        statement.bindLong(11, entity.actualMinutes.toLong())
        statement.bindLong(12, entity.parentId)
      }
    }
    this.__insertAdapterOfFocusHistoryEntity = object : EntityInsertAdapter<FocusHistoryEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `focus_history` (`date`,`totalFocusMinutes`,`tasksCompleted`,`sessionsCompleted`,`sessionsInterrupted`,`categoryDistribution`) VALUES (?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: FocusHistoryEntity) {
        statement.bindText(1, entity.date)
        statement.bindLong(2, entity.totalFocusMinutes.toLong())
        statement.bindLong(3, entity.tasksCompleted.toLong())
        statement.bindLong(4, entity.sessionsCompleted.toLong())
        statement.bindLong(5, entity.sessionsInterrupted.toLong())
        statement.bindText(6, entity.categoryDistribution)
      }
    }
    this.__insertAdapterOfHabitEntity = object : EntityInsertAdapter<HabitEntity>() {
      protected override fun createQuery(): String =
          "INSERT OR REPLACE INTO `habits` (`id`,`title`,`description`,`iconName`,`colorHex`,`streak`,`longestStreak`,`lastCompletedDate`,`createdAt`) VALUES (nullif(?, 0),?,?,?,?,?,?,?,?)"

      protected override fun bind(statement: SQLiteStatement, entity: HabitEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.description)
        statement.bindText(4, entity.iconName)
        statement.bindText(5, entity.colorHex)
        statement.bindLong(6, entity.streak.toLong())
        statement.bindLong(7, entity.longestStreak.toLong())
        statement.bindLong(8, entity.lastCompletedDate)
        statement.bindLong(9, entity.createdAt)
      }
    }
    this.__deleteAdapterOfTaskEntity = object : EntityDeleteOrUpdateAdapter<TaskEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `tasks` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__deleteAdapterOfHabitEntity = object : EntityDeleteOrUpdateAdapter<HabitEntity>() {
      protected override fun createQuery(): String = "DELETE FROM `habits` WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: HabitEntity) {
        statement.bindLong(1, entity.id)
      }
    }
    this.__updateAdapterOfTaskEntity = object : EntityDeleteOrUpdateAdapter<TaskEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `tasks` SET `id` = ?,`title` = ?,`notes` = ?,`category` = ?,`priority` = ?,`isCompleted` = ?,`dueDate` = ?,`rewardCoins` = ?,`energyLevel` = ?,`estimatedMinutes` = ?,`actualMinutes` = ?,`parentId` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: TaskEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.notes)
        statement.bindText(4, entity.category)
        statement.bindLong(5, entity.priority.toLong())
        val _tmp: Int = if (entity.isCompleted) 1 else 0
        statement.bindLong(6, _tmp.toLong())
        statement.bindLong(7, entity.dueDate)
        statement.bindLong(8, entity.rewardCoins.toLong())
        statement.bindLong(9, entity.energyLevel.toLong())
        statement.bindLong(10, entity.estimatedMinutes.toLong())
        statement.bindLong(11, entity.actualMinutes.toLong())
        statement.bindLong(12, entity.parentId)
        statement.bindLong(13, entity.id)
      }
    }
    this.__updateAdapterOfHabitEntity = object : EntityDeleteOrUpdateAdapter<HabitEntity>() {
      protected override fun createQuery(): String =
          "UPDATE OR ABORT `habits` SET `id` = ?,`title` = ?,`description` = ?,`iconName` = ?,`colorHex` = ?,`streak` = ?,`longestStreak` = ?,`lastCompletedDate` = ?,`createdAt` = ? WHERE `id` = ?"

      protected override fun bind(statement: SQLiteStatement, entity: HabitEntity) {
        statement.bindLong(1, entity.id)
        statement.bindText(2, entity.title)
        statement.bindText(3, entity.description)
        statement.bindText(4, entity.iconName)
        statement.bindText(5, entity.colorHex)
        statement.bindLong(6, entity.streak.toLong())
        statement.bindLong(7, entity.longestStreak.toLong())
        statement.bindLong(8, entity.lastCompletedDate)
        statement.bindLong(9, entity.createdAt)
        statement.bindLong(10, entity.id)
      }
    }
  }

  public override suspend fun insertTask(task: TaskEntity): Long = performSuspending(__db, false,
      true) { _connection ->
    val _result: Long = __insertAdapterOfTaskEntity.insertAndReturnId(_connection, task)
    _result
  }

  public override suspend fun insertFocusHistory(history: FocusHistoryEntity): Unit =
      performSuspending(__db, false, true) { _connection ->
    __insertAdapterOfFocusHistoryEntity.insert(_connection, history)
  }

  public override suspend fun insertHabit(habit: HabitEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __insertAdapterOfHabitEntity.insert(_connection, habit)
  }

  public override suspend fun deleteTask(task: TaskEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfTaskEntity.handle(_connection, task)
  }

  public override suspend fun deleteHabit(habit: HabitEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __deleteAdapterOfHabitEntity.handle(_connection, habit)
  }

  public override suspend fun updateTask(task: TaskEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfTaskEntity.handle(_connection, task)
  }

  public override suspend fun updateHabit(habit: HabitEntity): Unit = performSuspending(__db, false,
      true) { _connection ->
    __updateAdapterOfHabitEntity.handle(_connection, habit)
  }

  public override fun getAllTasks(): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks ORDER BY id DESC"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _cursorIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _cursorIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _cursorIndexOfRewardCoins: Int = getColumnIndexOrThrow(_stmt, "rewardCoins")
        val _cursorIndexOfEnergyLevel: Int = getColumnIndexOrThrow(_stmt, "energyLevel")
        val _cursorIndexOfEstimatedMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedMinutes")
        val _cursorIndexOfActualMinutes: Int = getColumnIndexOrThrow(_stmt, "actualMinutes")
        val _cursorIndexOfParentId: Int = getColumnIndexOrThrow(_stmt, "parentId")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_cursorIndexOfNotes)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_cursorIndexOfCategory)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_cursorIndexOfPriority).toInt()
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpDueDate: Long
          _tmpDueDate = _stmt.getLong(_cursorIndexOfDueDate)
          val _tmpRewardCoins: Int
          _tmpRewardCoins = _stmt.getLong(_cursorIndexOfRewardCoins).toInt()
          val _tmpEnergyLevel: Int
          _tmpEnergyLevel = _stmt.getLong(_cursorIndexOfEnergyLevel).toInt()
          val _tmpEstimatedMinutes: Int
          _tmpEstimatedMinutes = _stmt.getLong(_cursorIndexOfEstimatedMinutes).toInt()
          val _tmpActualMinutes: Int
          _tmpActualMinutes = _stmt.getLong(_cursorIndexOfActualMinutes).toInt()
          val _tmpParentId: Long
          _tmpParentId = _stmt.getLong(_cursorIndexOfParentId)
          _item =
              TaskEntity(_tmpId,_tmpTitle,_tmpNotes,_tmpCategory,_tmpPriority,_tmpIsCompleted,_tmpDueDate,_tmpRewardCoins,_tmpEnergyLevel,_tmpEstimatedMinutes,_tmpActualMinutes,_tmpParentId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getAllTasksOnce(): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _cursorIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _cursorIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _cursorIndexOfRewardCoins: Int = getColumnIndexOrThrow(_stmt, "rewardCoins")
        val _cursorIndexOfEnergyLevel: Int = getColumnIndexOrThrow(_stmt, "energyLevel")
        val _cursorIndexOfEstimatedMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedMinutes")
        val _cursorIndexOfActualMinutes: Int = getColumnIndexOrThrow(_stmt, "actualMinutes")
        val _cursorIndexOfParentId: Int = getColumnIndexOrThrow(_stmt, "parentId")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_cursorIndexOfNotes)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_cursorIndexOfCategory)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_cursorIndexOfPriority).toInt()
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpDueDate: Long
          _tmpDueDate = _stmt.getLong(_cursorIndexOfDueDate)
          val _tmpRewardCoins: Int
          _tmpRewardCoins = _stmt.getLong(_cursorIndexOfRewardCoins).toInt()
          val _tmpEnergyLevel: Int
          _tmpEnergyLevel = _stmt.getLong(_cursorIndexOfEnergyLevel).toInt()
          val _tmpEstimatedMinutes: Int
          _tmpEstimatedMinutes = _stmt.getLong(_cursorIndexOfEstimatedMinutes).toInt()
          val _tmpActualMinutes: Int
          _tmpActualMinutes = _stmt.getLong(_cursorIndexOfActualMinutes).toInt()
          val _tmpParentId: Long
          _tmpParentId = _stmt.getLong(_cursorIndexOfParentId)
          _item =
              TaskEntity(_tmpId,_tmpTitle,_tmpNotes,_tmpCategory,_tmpPriority,_tmpIsCompleted,_tmpDueDate,_tmpRewardCoins,_tmpEnergyLevel,_tmpEstimatedMinutes,_tmpActualMinutes,_tmpParentId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getSubTasks(parentId: Long): Flow<List<TaskEntity>> {
    val _sql: String = "SELECT * FROM tasks WHERE parentId = ?"
    return createFlow(__db, false, arrayOf("tasks")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, parentId)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _cursorIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _cursorIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _cursorIndexOfRewardCoins: Int = getColumnIndexOrThrow(_stmt, "rewardCoins")
        val _cursorIndexOfEnergyLevel: Int = getColumnIndexOrThrow(_stmt, "energyLevel")
        val _cursorIndexOfEstimatedMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedMinutes")
        val _cursorIndexOfActualMinutes: Int = getColumnIndexOrThrow(_stmt, "actualMinutes")
        val _cursorIndexOfParentId: Int = getColumnIndexOrThrow(_stmt, "parentId")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_cursorIndexOfNotes)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_cursorIndexOfCategory)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_cursorIndexOfPriority).toInt()
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpDueDate: Long
          _tmpDueDate = _stmt.getLong(_cursorIndexOfDueDate)
          val _tmpRewardCoins: Int
          _tmpRewardCoins = _stmt.getLong(_cursorIndexOfRewardCoins).toInt()
          val _tmpEnergyLevel: Int
          _tmpEnergyLevel = _stmt.getLong(_cursorIndexOfEnergyLevel).toInt()
          val _tmpEstimatedMinutes: Int
          _tmpEstimatedMinutes = _stmt.getLong(_cursorIndexOfEstimatedMinutes).toInt()
          val _tmpActualMinutes: Int
          _tmpActualMinutes = _stmt.getLong(_cursorIndexOfActualMinutes).toInt()
          val _tmpParentId: Long
          _tmpParentId = _stmt.getLong(_cursorIndexOfParentId)
          _item =
              TaskEntity(_tmpId,_tmpTitle,_tmpNotes,_tmpCategory,_tmpPriority,_tmpIsCompleted,_tmpDueDate,_tmpRewardCoins,_tmpEnergyLevel,_tmpEstimatedMinutes,_tmpActualMinutes,_tmpParentId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getSubTasksOnce(parentId: Long): List<TaskEntity> {
    val _sql: String = "SELECT * FROM tasks WHERE parentId = ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, parentId)
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfNotes: Int = getColumnIndexOrThrow(_stmt, "notes")
        val _cursorIndexOfCategory: Int = getColumnIndexOrThrow(_stmt, "category")
        val _cursorIndexOfPriority: Int = getColumnIndexOrThrow(_stmt, "priority")
        val _cursorIndexOfIsCompleted: Int = getColumnIndexOrThrow(_stmt, "isCompleted")
        val _cursorIndexOfDueDate: Int = getColumnIndexOrThrow(_stmt, "dueDate")
        val _cursorIndexOfRewardCoins: Int = getColumnIndexOrThrow(_stmt, "rewardCoins")
        val _cursorIndexOfEnergyLevel: Int = getColumnIndexOrThrow(_stmt, "energyLevel")
        val _cursorIndexOfEstimatedMinutes: Int = getColumnIndexOrThrow(_stmt, "estimatedMinutes")
        val _cursorIndexOfActualMinutes: Int = getColumnIndexOrThrow(_stmt, "actualMinutes")
        val _cursorIndexOfParentId: Int = getColumnIndexOrThrow(_stmt, "parentId")
        val _result: MutableList<TaskEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: TaskEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpNotes: String
          _tmpNotes = _stmt.getText(_cursorIndexOfNotes)
          val _tmpCategory: String
          _tmpCategory = _stmt.getText(_cursorIndexOfCategory)
          val _tmpPriority: Int
          _tmpPriority = _stmt.getLong(_cursorIndexOfPriority).toInt()
          val _tmpIsCompleted: Boolean
          val _tmp: Int
          _tmp = _stmt.getLong(_cursorIndexOfIsCompleted).toInt()
          _tmpIsCompleted = _tmp != 0
          val _tmpDueDate: Long
          _tmpDueDate = _stmt.getLong(_cursorIndexOfDueDate)
          val _tmpRewardCoins: Int
          _tmpRewardCoins = _stmt.getLong(_cursorIndexOfRewardCoins).toInt()
          val _tmpEnergyLevel: Int
          _tmpEnergyLevel = _stmt.getLong(_cursorIndexOfEnergyLevel).toInt()
          val _tmpEstimatedMinutes: Int
          _tmpEstimatedMinutes = _stmt.getLong(_cursorIndexOfEstimatedMinutes).toInt()
          val _tmpActualMinutes: Int
          _tmpActualMinutes = _stmt.getLong(_cursorIndexOfActualMinutes).toInt()
          val _tmpParentId: Long
          _tmpParentId = _stmt.getLong(_cursorIndexOfParentId)
          _item =
              TaskEntity(_tmpId,_tmpTitle,_tmpNotes,_tmpCategory,_tmpPriority,_tmpIsCompleted,_tmpDueDate,_tmpRewardCoins,_tmpEnergyLevel,_tmpEstimatedMinutes,_tmpActualMinutes,_tmpParentId)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getFocusHistory(startDate: String): Flow<List<FocusHistoryEntity>> {
    val _sql: String = "SELECT * FROM focus_history WHERE date >= ?"
    return createFlow(__db, false, arrayOf("focus_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, startDate)
        val _cursorIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _cursorIndexOfTotalFocusMinutes: Int = getColumnIndexOrThrow(_stmt, "totalFocusMinutes")
        val _cursorIndexOfTasksCompleted: Int = getColumnIndexOrThrow(_stmt, "tasksCompleted")
        val _cursorIndexOfSessionsCompleted: Int = getColumnIndexOrThrow(_stmt, "sessionsCompleted")
        val _cursorIndexOfSessionsInterrupted: Int = getColumnIndexOrThrow(_stmt,
            "sessionsInterrupted")
        val _cursorIndexOfCategoryDistribution: Int = getColumnIndexOrThrow(_stmt,
            "categoryDistribution")
        val _result: MutableList<FocusHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FocusHistoryEntity
          val _tmpDate: String
          _tmpDate = _stmt.getText(_cursorIndexOfDate)
          val _tmpTotalFocusMinutes: Int
          _tmpTotalFocusMinutes = _stmt.getLong(_cursorIndexOfTotalFocusMinutes).toInt()
          val _tmpTasksCompleted: Int
          _tmpTasksCompleted = _stmt.getLong(_cursorIndexOfTasksCompleted).toInt()
          val _tmpSessionsCompleted: Int
          _tmpSessionsCompleted = _stmt.getLong(_cursorIndexOfSessionsCompleted).toInt()
          val _tmpSessionsInterrupted: Int
          _tmpSessionsInterrupted = _stmt.getLong(_cursorIndexOfSessionsInterrupted).toInt()
          val _tmpCategoryDistribution: String
          _tmpCategoryDistribution = _stmt.getText(_cursorIndexOfCategoryDistribution)
          _item =
              FocusHistoryEntity(_tmpDate,_tmpTotalFocusMinutes,_tmpTasksCompleted,_tmpSessionsCompleted,_tmpSessionsInterrupted,_tmpCategoryDistribution)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getFocusHistoryOnce(startDate: String): List<FocusHistoryEntity> {
    val _sql: String = "SELECT * FROM focus_history WHERE date >= ?"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, startDate)
        val _cursorIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _cursorIndexOfTotalFocusMinutes: Int = getColumnIndexOrThrow(_stmt, "totalFocusMinutes")
        val _cursorIndexOfTasksCompleted: Int = getColumnIndexOrThrow(_stmt, "tasksCompleted")
        val _cursorIndexOfSessionsCompleted: Int = getColumnIndexOrThrow(_stmt, "sessionsCompleted")
        val _cursorIndexOfSessionsInterrupted: Int = getColumnIndexOrThrow(_stmt,
            "sessionsInterrupted")
        val _cursorIndexOfCategoryDistribution: Int = getColumnIndexOrThrow(_stmt,
            "categoryDistribution")
        val _result: MutableList<FocusHistoryEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: FocusHistoryEntity
          val _tmpDate: String
          _tmpDate = _stmt.getText(_cursorIndexOfDate)
          val _tmpTotalFocusMinutes: Int
          _tmpTotalFocusMinutes = _stmt.getLong(_cursorIndexOfTotalFocusMinutes).toInt()
          val _tmpTasksCompleted: Int
          _tmpTasksCompleted = _stmt.getLong(_cursorIndexOfTasksCompleted).toInt()
          val _tmpSessionsCompleted: Int
          _tmpSessionsCompleted = _stmt.getLong(_cursorIndexOfSessionsCompleted).toInt()
          val _tmpSessionsInterrupted: Int
          _tmpSessionsInterrupted = _stmt.getLong(_cursorIndexOfSessionsInterrupted).toInt()
          val _tmpCategoryDistribution: String
          _tmpCategoryDistribution = _stmt.getText(_cursorIndexOfCategoryDistribution)
          _item =
              FocusHistoryEntity(_tmpDate,_tmpTotalFocusMinutes,_tmpTasksCompleted,_tmpSessionsCompleted,_tmpSessionsInterrupted,_tmpCategoryDistribution)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun getFocusHistoryByDate(date: String): FocusHistoryEntity? {
    val _sql: String = "SELECT * FROM focus_history WHERE date = ? LIMIT 1"
    return performSuspending(__db, true, false) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        val _cursorIndexOfDate: Int = getColumnIndexOrThrow(_stmt, "date")
        val _cursorIndexOfTotalFocusMinutes: Int = getColumnIndexOrThrow(_stmt, "totalFocusMinutes")
        val _cursorIndexOfTasksCompleted: Int = getColumnIndexOrThrow(_stmt, "tasksCompleted")
        val _cursorIndexOfSessionsCompleted: Int = getColumnIndexOrThrow(_stmt, "sessionsCompleted")
        val _cursorIndexOfSessionsInterrupted: Int = getColumnIndexOrThrow(_stmt,
            "sessionsInterrupted")
        val _cursorIndexOfCategoryDistribution: Int = getColumnIndexOrThrow(_stmt,
            "categoryDistribution")
        val _result: FocusHistoryEntity?
        if (_stmt.step()) {
          val _tmpDate: String
          _tmpDate = _stmt.getText(_cursorIndexOfDate)
          val _tmpTotalFocusMinutes: Int
          _tmpTotalFocusMinutes = _stmt.getLong(_cursorIndexOfTotalFocusMinutes).toInt()
          val _tmpTasksCompleted: Int
          _tmpTasksCompleted = _stmt.getLong(_cursorIndexOfTasksCompleted).toInt()
          val _tmpSessionsCompleted: Int
          _tmpSessionsCompleted = _stmt.getLong(_cursorIndexOfSessionsCompleted).toInt()
          val _tmpSessionsInterrupted: Int
          _tmpSessionsInterrupted = _stmt.getLong(_cursorIndexOfSessionsInterrupted).toInt()
          val _tmpCategoryDistribution: String
          _tmpCategoryDistribution = _stmt.getText(_cursorIndexOfCategoryDistribution)
          _result =
              FocusHistoryEntity(_tmpDate,_tmpTotalFocusMinutes,_tmpTasksCompleted,_tmpSessionsCompleted,_tmpSessionsInterrupted,_tmpCategoryDistribution)
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalFocusMinutes(): Flow<Int?> {
    val _sql: String = "SELECT SUM(totalFocusMinutes) FROM focus_history"
    return createFlow(__db, false, arrayOf("focus_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int?
        if (_stmt.step()) {
          val _tmp: Int?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0).toInt()
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getTotalTasksCompleted(): Flow<Int?> {
    val _sql: String = "SELECT SUM(tasksCompleted) FROM focus_history"
    return createFlow(__db, false, arrayOf("focus_history")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _result: Int?
        if (_stmt.step()) {
          val _tmp: Int?
          if (_stmt.isNull(0)) {
            _tmp = null
          } else {
            _tmp = _stmt.getLong(0).toInt()
          }
          _result = _tmp
        } else {
          _result = null
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override fun getAllHabits(): Flow<List<HabitEntity>> {
    val _sql: String = "SELECT * FROM habits"
    return createFlow(__db, false, arrayOf("habits")) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        val _cursorIndexOfId: Int = getColumnIndexOrThrow(_stmt, "id")
        val _cursorIndexOfTitle: Int = getColumnIndexOrThrow(_stmt, "title")
        val _cursorIndexOfDescription: Int = getColumnIndexOrThrow(_stmt, "description")
        val _cursorIndexOfIconName: Int = getColumnIndexOrThrow(_stmt, "iconName")
        val _cursorIndexOfColorHex: Int = getColumnIndexOrThrow(_stmt, "colorHex")
        val _cursorIndexOfStreak: Int = getColumnIndexOrThrow(_stmt, "streak")
        val _cursorIndexOfLongestStreak: Int = getColumnIndexOrThrow(_stmt, "longestStreak")
        val _cursorIndexOfLastCompletedDate: Int = getColumnIndexOrThrow(_stmt, "lastCompletedDate")
        val _cursorIndexOfCreatedAt: Int = getColumnIndexOrThrow(_stmt, "createdAt")
        val _result: MutableList<HabitEntity> = mutableListOf()
        while (_stmt.step()) {
          val _item: HabitEntity
          val _tmpId: Long
          _tmpId = _stmt.getLong(_cursorIndexOfId)
          val _tmpTitle: String
          _tmpTitle = _stmt.getText(_cursorIndexOfTitle)
          val _tmpDescription: String
          _tmpDescription = _stmt.getText(_cursorIndexOfDescription)
          val _tmpIconName: String
          _tmpIconName = _stmt.getText(_cursorIndexOfIconName)
          val _tmpColorHex: String
          _tmpColorHex = _stmt.getText(_cursorIndexOfColorHex)
          val _tmpStreak: Int
          _tmpStreak = _stmt.getLong(_cursorIndexOfStreak).toInt()
          val _tmpLongestStreak: Int
          _tmpLongestStreak = _stmt.getLong(_cursorIndexOfLongestStreak).toInt()
          val _tmpLastCompletedDate: Long
          _tmpLastCompletedDate = _stmt.getLong(_cursorIndexOfLastCompletedDate)
          val _tmpCreatedAt: Long
          _tmpCreatedAt = _stmt.getLong(_cursorIndexOfCreatedAt)
          _item =
              HabitEntity(_tmpId,_tmpTitle,_tmpDescription,_tmpIconName,_tmpColorHex,_tmpStreak,_tmpLongestStreak,_tmpLastCompletedDate,_tmpCreatedAt)
          _result.add(_item)
        }
        _result
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteCompletedTasks() {
    val _sql: String = "DELETE FROM tasks WHERE isCompleted = 1"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun deleteSubTasks(parentId: Long) {
    val _sql: String = "DELETE FROM tasks WHERE parentId = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, parentId)
        _stmt.step()
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun updateFocusHistoryAtomic(
    date: String,
    focusMins: Int,
    tasksDone: Int,
    sessionsComp: Int,
    sessionsInt: Int,
  ): Int {
    val _sql: String =
        "UPDATE focus_history SET totalFocusMinutes = totalFocusMinutes + ?, tasksCompleted = tasksCompleted + ?, sessionsCompleted = sessionsCompleted + ?, sessionsInterrupted = sessionsInterrupted + ? WHERE date = ?"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindLong(_argIndex, focusMins.toLong())
        _argIndex = 2
        _stmt.bindLong(_argIndex, tasksDone.toLong())
        _argIndex = 3
        _stmt.bindLong(_argIndex, sessionsComp.toLong())
        _argIndex = 4
        _stmt.bindLong(_argIndex, sessionsInt.toLong())
        _argIndex = 5
        _stmt.bindText(_argIndex, date)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public override suspend fun recoverInterruptedSession(date: String): Int {
    val _sql: String =
        "UPDATE focus_history SET sessionsCompleted = sessionsCompleted + 1, sessionsInterrupted = sessionsInterrupted - 1 WHERE date = ? AND sessionsInterrupted > 0"
    return performSuspending(__db, false, true) { _connection ->
      val _stmt: SQLiteStatement = _connection.prepare(_sql)
      try {
        var _argIndex: Int = 1
        _stmt.bindText(_argIndex, date)
        _stmt.step()
        getTotalChangedRows(_connection)
      } finally {
        _stmt.close()
      }
    }
  }

  public companion object {
    public fun getRequiredConverters(): List<KClass<*>> = emptyList()
  }
}
