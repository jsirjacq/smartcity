package be.info.unamur.persistence.entities

import java.sql.Timestamp

import scalikejdbc._


/**
  * @author Noé Picard
  */
case class Sensor(id: Long,
                  name: String,
                  value: Double,
                  grossValue: Double,
                  createdAt: Timestamp)

object Sensor extends SQLSyntaxSupport[Sensor] {
  override val tableName = "sensors"

  override val columns = Seq("id", "name", "value", "gross_value", "created_at")

  override val autoSession = AutoSession

  val sensor = Sensor.syntax("s")


  def apply(s: ResultName[Sensor])(rs: WrappedResultSet): Sensor = new Sensor(
    id = rs.int(s.id),
    name = rs.string(s.name),
    value = rs.double(s.value),
    grossValue = rs.double(s.grossValue),
    createdAt = rs.timestamp(s.createdAt)
  )

  def find(id: Int)(implicit session: DBSession = autoSession): Option[Sensor] = {
    withSQL {
      select.from(Sensor as sensor).where.eq(sensor.id, id)
    }.map(Sensor(sensor.resultName)).single.apply()
  }

  def findAll()(implicit session: DBSession = autoSession): List[Sensor] = {
    withSQL(select.from(Sensor as sensor)).map(Sensor(sensor.resultName)).list.apply()
  }

  def countAll()(implicit session: DBSession = autoSession): Long = {
    withSQL(select(sqls"count(1)").from(Sensor as sensor)).map(rs => rs.long(1)).single.apply().get
  }

  def findAllBy(where: SQLSyntax)(implicit session: DBSession = autoSession): List[Sensor] = {
    withSQL {
      select.from(Sensor as sensor).where.append(sqls"$where")
    }.map(Sensor(sensor.resultName)).list.apply()
  }

  def countBy(where: SQLSyntax)(implicit session: DBSession = autoSession): Long = {
    withSQL {
      select(sqls"count(1)").from(Sensor as sensor).where.append(sqls"$where")
    }.map(_.long(1)).single.apply().get
  }

  def create(name: String,
             value: Double,
             grossValue: Double,
             createdAt: Timestamp)(implicit session: DBSession = autoSession): Sensor = {
    val generatedKey = withSQL {
      insert.into(Sensor).columns(
        column.name,
        column.value,
        column.grossValue,
        column.createdAt
      ).values(
        name,
        value,
        grossValue,
        createdAt)
    }.updateAndReturnGeneratedKey.apply()

    Sensor(
      id = generatedKey.toInt,
      name = name,
      value = value,
      grossValue = grossValue,
      createdAt = createdAt)
  }

  def save(s: Sensor)(implicit session: DBSession = autoSession): Sensor = {
    withSQL {
      update(Sensor as sensor).set(
        sensor.id -> s.id,
        sensor.name -> s.name,
        sensor.value -> s.value,
        sensor.grossValue -> s.grossValue,
        sensor.createdAt -> s.createdAt
      )
    }.update().apply()
    s
  }

  def destroy(s: Sensor)(implicit session: DBSession = autoSession): Unit = {
    withSQL {
      delete.from(Sensor).where.eq(column.id, s.id)
    }.update.apply()
  }
}
