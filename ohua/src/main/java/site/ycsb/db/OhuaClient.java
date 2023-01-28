/**
 * Copyright (c) 2014-2015 YCSB contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you
 * may not use this file except in compliance with the License. You
 * may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * permissions and limitations under the License. See accompanying
 * LICENSE file.
 */

package site.ycsb.db;

import com.github.cliftonlabs.json_simple.JsonObject;
import com.github.cliftonlabs.json_simple.Jsoner;
import org.apache.log4j.Logger;
import site.ycsb.ByteIterator;
import site.ycsb.DB;
import site.ycsb.DBException;
import site.ycsb.Status;
import site.ycsb.StringByteIterator;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.*;

/**
 * Concrete client implementation.
 */
public class OhuaClient extends DB {

  private final Logger logger = Logger.getLogger(getClass());

  // smoltcp seems to assume rust based OS and implements loopback entirely.
  // as a rust struct, so I'll provide a tuntap address here. Also getting host by IP
  // might throw so I moved it interim wise into the try blocks.
  public static final int DEFAULT_PORT = 6969;
  private static InetAddress serverIP;
  private static Socket socket;
  // Closing an InputStream closes the socket.
  // As I also can not open just a bunch of them I'll need to keep
  // both I think.
  private static InputStream inputStream;

  @Override
  public void init() throws DBException{
    try {
      serverIP = InetAddress.getByName("192.168.69.1");
      socket = new Socket(serverIP, DEFAULT_PORT);
      inputStream = socket.getInputStream();
    } catch (Exception e) {
      System.out.println("Establishing Socket failed");
      throw new DBException("Can not establish connection");
    }
  }

  @Override 
  public void cleanup() throws DBException {
    try {
      socket.close();
    } catch (Exception e) {
      System.out.println("Closing Socket failed");
      throw new DBException("Can not close connection");
    }
    
  }

  @Override
  public Status read(
      String table, String key, Set<String> fields,
      Map<String, ByteIterator> result) {
    key = createQualifiedKey(table, key);

    // generate JSON
    JsonObject obj = new JsonObject();
    obj.put("table", table);
    obj.put("key", key);
    JsonObject req = new JsonObject();
    req.put("Read", obj);

    // send the actual request
    JsonObject response;
    try {
      // send data
  
      OutputStream output = socket.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());

      // retrieve response
      //InputStream input = sock.getInputStream();
      byte[] responseBuffer = new byte[2];  
      int num = inputStream.readNBytes(responseBuffer, 0, 2);
      String resultString = new String(responseBuffer);
      //System.err.println("Got input read");
      //response = (JsonObject) Jsoner.deserialize(resultString);
      // ! For now just check if we received OK
      if (resultString.compareTo("OK") != 0) {
        System.out.println(resultString);
        return Status.ERROR;
      }

    } catch (Exception e) {
      System.err.println("Error encountered for key: " + key + " " + e);
      return Status.ERROR;
    }

    // parse response
    /*
    boolean checkFields = fields != null && !fields.isEmpty();
    Map<String, String> entries = (Map<String, String>) response.get("value");
    for (Map.Entry<String, String> item: entries.entrySet()) {
      // skip element if only a specific subset was asked for
      if (checkFields && !fields.contains(item.getKey())) {
        continue;
      }
      String val = item.getValue();
      if (val != null) {
        result.put(item.getKey(), new StringByteIterator(val));
      }
    }*/


    return Status.OK;
  }

  @Override
  public Status scan(
      String table, String startkey, int recordcount, Set<String> fields,
      Vector<HashMap<String, ByteIterator>> result){
    return Status.NOT_IMPLEMENTED;
  }

  @Override
  public Status update(
      String table, String key, Map<String, ByteIterator> values) {
    key = createQualifiedKey(table, key);
    // transform the values map because otherwise it's not serializable
    Map<String, String> stringMap = StringByteIterator.getStringMap(values);

    // generate JSON
    JsonObject obj = new JsonObject();
    obj.put("table", table);
    obj.put("key", key);
    obj.put("value", stringMap);
    JsonObject req = new JsonObject();
    req.put("Update", obj);

    // send the actual request
    try {
      // send data
      OutputStream output = socket.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());
      output.flush();

      // retrieve response
      byte[] result = new byte[2];  
      int num = inputStream.readNBytes(result, 0, 2);
      String s = new String(result);
      //System.err.println("Got input update");
      if (s.compareTo("OK") != 0) {
        //System.out.println(s);
        return Status.ERROR;
      }
      //sock.close();      
    } catch (Exception e) {
      System.err.println("Error encountered for key: " + key + " " + e);
      //logger.error("Error encountered for key: " + key, e);
      return Status.ERROR;
    }

    return Status.OK;
  }

  @Override
  public Status insert(
      String table, String key, Map<String, ByteIterator> values) {
    key = createQualifiedKey(table, key);
    // transform the values map because otherwise it's not serializable
    Map<String, String> stringMap = StringByteIterator.getStringMap(values);

    // generate JSON
    JsonObject obj = new JsonObject();
    obj.put("table", table);
    obj.put("key", key);
    obj.put("value", stringMap);
    JsonObject req = new JsonObject();
    req.put("Write", obj);

    // send the actual request
    try {
      // send data
      OutputStream output = socket.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());
      output.flush();
      //System.err.println("wrote to socket write ");

      // retrieve response
      byte[] result = new byte[2];  
      int num = inputStream.readNBytes(result, 0, 2);
      String s = new String(result);
      //System.err.println("Got input insert");
      if (s.compareTo("OK") != 0) {
        System.out.println(s);
        return Status.ERROR;
      } 
      //sock.close();     
    } catch (Exception e) {
      System.err.println("Error encountered for key: " + key + " " + e);
      //logger.error("Error encountered for key: " + key, e);
      return Status.ERROR;
    }
    
    return Status.OK;
  }

  @Override
  public Status delete(String table, String key) {
    key = createQualifiedKey(table, key);

    // generate JSON
    JsonObject obj = new JsonObject();
    obj.put("table", table);
    obj.put("key", key);
    JsonObject req = new JsonObject();
    req.put("Delete", obj);

    // send the actual request
    try { 
      // send data
      OutputStream output = socket.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());
      output.flush();

      // retrieve response
      byte[] result = new byte[2];  
      int num = inputStream.readNBytes(result, 0, 2);
      String s = new String(result);
      //System.err.println("Got input delete");
      if (s.compareTo("OK") != 0) {
        System.out.println(s);
        return Status.ERROR;
      }
    } catch (Exception e) {
      System.err.println("Error encountered for key: " + key + " " + e);
      return Status.ERROR;
    }
    return Status.OK;
  }

  protected static String createQualifiedKey(String table, String key) {
    return MessageFormat.format("{0}-{1}", table, key);
  }
}
