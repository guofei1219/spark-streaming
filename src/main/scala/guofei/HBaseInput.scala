package guofei

import org.apache.hadoop.hbase.client.HBaseAdmin
import org.apache.hadoop.hbase.{HBaseConfiguration, HTableDescriptor, TableName}
import org.apache.hadoop.hbase.mapreduce.TableInputFormat
import org.apache.hadoop.hbase.KeyValue.Type
import org.apache.hadoop.hbase.HConstants
import org.apache.hadoop.hbase.util.Bytes
import org.apache.hadoop.hbase.CellUtil
import org.apache.spark._
import scala.collection.JavaConverters._
/**
  * Created by 郭飞 on 2016/5/26.
  */
object HBaseInput {
  def main(args: Array[String]) {
    val sparkConf = new SparkConf().setAppName("HBaseTest").setMaster("local[2]")
    val sc = new SparkContext(sparkConf)
    val conf = HBaseConfiguration.create()
    val zk = "192.168.20.131"
    val table = "hb_trdpty_json_result"
    // Other options for hbase configuration are available, please check
    // http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/HConstants.html
    conf.set(HConstants.ZOOKEEPER_QUORUM, zk)
    // Other options for configuring scan behavior are available. More information available at
    // http://hbase.apache.org/apidocs/org/apache/hadoop/hbase/mapreduce/TableInputFormat.html
    conf.set(TableInputFormat.INPUT_TABLE, table)

    // Initialize hBase table if necessary
    val admin = new HBaseAdmin(conf)
    if (!admin.isTableAvailable(table)) {
      val tableDesc = new HTableDescriptor(TableName.valueOf(table))
      admin.createTable(tableDesc)
    }

    val hBaseRDD = sc.newAPIHadoopRDD(conf, classOf[TableInputFormat],
      classOf[org.apache.hadoop.hbase.io.ImmutableBytesWritable],
      classOf[org.apache.hadoop.hbase.client.Result])

    //keyValue is a RDD[java.util.list[hbase.KeyValue]]
    val keyValue = hBaseRDD.map(x => x._2).map(_.list)

    //outPut is a RDD[String], in which each line represents a record in HBase
    val outPut = keyValue.flatMap(x =>  x.asScala.map(cell =>
      "columnFamily=%s,qualifier=%s,timestamp=%s,type=%s,value=%s".format(
        Bytes.toStringBinary(CellUtil.cloneFamily(cell)),
        Bytes.toStringBinary(CellUtil.cloneQualifier(cell)),
        cell.getTimestamp.toString,
        Type.codeToType(cell.getTypeByte),
        Bytes.toStringBinary(CellUtil.cloneValue(cell))
      )
    )
    )

    outPut.foreach(println)

    sc.stop()
  }
}