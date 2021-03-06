package com.inmobi.databus.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;
import java.util.TreeMap;
import java.util.zip.GZIPInputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.fs.PathFilter;

import com.inmobi.databus.Cluster;
import com.inmobi.databus.files.CollectorFile;
import com.inmobi.databus.files.DatabusStreamFile;
import com.inmobi.databus.files.FileMap;
import com.inmobi.databus.partition.PartitionCheckpoint;
import com.inmobi.databus.partition.PartitionId;

public abstract class DatabusStreamReader extends StreamReader<DatabusStreamFile> {

  private static final Log LOG = LogFactory.getLog(DatabusStreamReader.class);

  protected Date buildTimestamp;

  public Date getBuildTimestamp() {
    return buildTimestamp;
  }

  public void setBuildTimestamp(Date buildTimestamp) {
    this.buildTimestamp = buildTimestamp;
  }

  DatabusStreamReader(PartitionId partitionId, Cluster cluster, 
      String streamName) throws IOException {
    // initialize cluster and its directories
    super(partitionId, cluster, streamName);
  }

  abstract class StreamFileMap extends FileMap<DatabusStreamFile> {
    @Override
    protected void buildList() throws IOException {
      buildListing(pathFilter);
    }
    
    @Override
    protected TreeMap<DatabusStreamFile, Path> createFilesMap() {
      return new TreeMap<DatabusStreamFile, Path>();
    }

    @Override
    protected DatabusStreamFile getStreamFile(String fileName) {
      return DatabusStreamFile.create(streamName, fileName);
    }

  };

  public void build(Date date) throws IOException {
    setBuildTimestamp(date);
    build();
  }

  void buildListing(PathFilter pathFilter) throws IOException {
    Calendar current = Calendar.getInstance();
    Date now = current.getTime();
    current.setTime(buildTimestamp);
    while (current.getTime().before(now)) {
      Path hhDir =  new Path(streamDir, hhDirFormat.get().format(
          current.getTime()));
      int hour = current.get(Calendar.HOUR_OF_DAY);
      if (fs.exists(hhDir)) {
        while (current.getTime().before(now) && 
            hour  == current.get(Calendar.HOUR_OF_DAY)) {
          Path dir = new Path(streamDir, minDirFormat.get().format(
              current.getTime()));
          LOG.debug("Current dir :" + dir);
          // Move the current minute to next minute
          current.add(Calendar.MINUTE, 1);
          FileStatus[] fileStatuses = fs.listStatus(dir, pathFilter);
          if (fileStatuses == null || fileStatuses.length == 0) {
            LOG.debug("No files in directory:" + dir);
          } else {
            for (FileStatus file : fileStatuses) {
              fileMap.addPath(file.getPath());
            }
          }
        } 
      } else {
        // go to next hour
        current.add(Calendar.HOUR_OF_DAY, 1);
        current.set(Calendar.MINUTE, 0);
      }
    }
  }

  /**
   *  Comment out this method if partition reader should not read from start of
   *   stream
   *  if check point does not exist.
   */
  public boolean initializeCurrentFile(PartitionCheckpoint checkpoint)
      throws IOException {
    boolean ret = super.initializeCurrentFile(checkpoint);
    if (!ret) {
      LOG.info("Could not find checkpointed file: " + checkpoint.getFileName());
      if (fileMap.isBefore(checkpoint.getFileName())) {
        LOG.info("Reading from start of the stream");
        return initFromStart();
      }
    }
    return ret;
  }

  protected BufferedReader createReader(FSDataInputStream in)
      throws IOException {
    BufferedReader reader = new BufferedReader(new InputStreamReader(
        new GZIPInputStream(in)));
    return reader;
  }

  protected void skipOldData(FSDataInputStream in, BufferedReader reader)
      throws IOException {
    skipLines(in, reader, currentLineNum);
  }

  @Override
  protected String getStreamFileName(String streamName, Date timestamp) {
    return getDatabusStreamFileName(streamName, timestamp);
  }

  public boolean isStreamFile(String fileName) {
    return isDatabusStreamFile(streamName, fileName);
  }

  static Date getDateFromStreamFile(String streamName,
      String fileName) throws Exception {
    return getDatabusStreamFile(streamName,
        fileName).getCollectorFile().getTimestamp();
  }

  public static Date getBuildTimestamp(String streamName,
      String streamFileName) {
    try {
      return getDateFromStreamFile(streamName,  streamFileName);
    } catch (Exception e) {
      throw new IllegalArgumentException("Invalid fileName:" + 
          streamFileName, e);
    }
  }

  static Date getDateFromDatabusStreamFile(String streamName, String fileName) {
    return DatabusStreamFile.create(streamName, fileName).getCollectorFile()
        .getTimestamp();
  }


  static boolean isDatabusStreamFile(String streamName, String fileName) {
    try {
      getDatabusStreamFile(streamName, fileName);
    } catch (IllegalArgumentException ie) {
      return false;
    }
    return true;
  }

  static String getDatabusStreamFileName(String streamName,
      Date date) {
    return getDatabusStreamFile(streamName, date).toString();  
  }

  static DatabusStreamFile getDatabusStreamFile(String streamName,
      Date date) {
    return new DatabusStreamFile("", new CollectorFile(streamName, date, 0),
        "gz");  
  }

  static DatabusStreamFile getDatabusStreamFile(String streamName,
      String fileName) {
    return DatabusStreamFile.create(streamName, fileName);  
  }

  public static String getDatabusStreamFileName(String collector,
      String collectorFile) {
    return collector + "-" + collectorFile + ".gz";  
  }

}
