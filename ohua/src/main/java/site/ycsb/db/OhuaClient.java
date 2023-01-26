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
import site.ycsb.Status;
import site.ycsb.StringByteIterator;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.InetAddress;
import java.text.MessageFormat;
import java.util.*;

/**
 * Concrete Memcached client implementation.
 */
public class OhuaClient extends DB {

  private final Logger logger = Logger.getLogger(getClass());

  // smoltcp seems to assume rust based OS and implements loopback entirely
  // as a rust struct, so I'll provide a tuntap address here. Also getting host by IP
  // might throw so I moved it interim wise into the try blocks
  // public static final InetAddress address = InetAddress.getByNa("192.168.69.1");
  public static final int DEFAULT_PORT = 6969;

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
      InetAddress address = InetAddress.getByName("192.168.69.1"); //InetAddress.getByName("127.0.0.1");
      Socket sock = new Socket(address, DEFAULT_PORT);
      System.err.println("opened socket read");
      // send data
      OutputStream output = sock.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());

      // retrieve response
      InputStream input = sock.getInputStream();
      byte[] responseBuffer = new byte[2];  
      //int num = input.readNBytes(responseBuffer, 0, 2);
      String resultString = new String(input.readAllBytes());
      System.err.println("Got input");
      response = (JsonObject) Jsoner.deserialize(resultString);
      sock.close();

    } catch (Exception e) {
      System.err.println("Error encountered for key: " + key + " " + e);
      //logger.error("Error encountered for key: " + key, e);
      return Status.ERROR;
    }

    // parse response
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
    }

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
      InetAddress address = InetAddress.getByName("192.168.69.1"); //InetAddress.getByName("127.0.0.1");
      Socket sock = new Socket(address, DEFAULT_PORT);
      System.err.println("opened socket update");
      // send data
      OutputStream output = sock.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());

      // retrieve response
      InputStream input = sock.getInputStream();
      byte[] result = new byte[2];  
      //int num = input.readNBytes(result, 0, 2);
      String s = new String(input.readAllBytes());
      System.err.println("Got input");
      if (s.compareTo("OK") != 0) {
        System.out.println(s);
        return Status.ERROR;
      }

      sock.close();
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
      InetAddress address = InetAddress.getByName("192.168.69.1"); //InetAddress.getByName("127.0.0.1");
      Socket sock = new Socket(address, DEFAULT_PORT);
      System.err.println("opened socket write ");
      // send data
      OutputStream output = sock.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());
      System.err.println("wrote to socket write ");

      // retrieve response
      InputStream input = sock.getInputStream();
      byte[] result = new byte[2];  
      //int num = input.readNBytes(result, 0, 2);
      String s = new String(input.readAllBytes());
      System.err.println("Got input");
      if (s.compareTo("OK") != 0) {
        System.out.println(s);
        return Status.ERROR;
      }

      sock.close();
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
      InetAddress address = InetAddress.getByName("192.168.69.1"); //InetAddress.getByName("127.0.0.1");
      Socket sock = new Socket(address, DEFAULT_PORT);
      System.err.println("opened socket delete");
      // send data
      OutputStream output = sock.getOutputStream();
      output.write(Jsoner.serialize(req).getBytes());

      // retrieve response
      InputStream input = sock.getInputStream();
      byte[] result = new byte[2];  
      //int num = input.readNBytes(result, 0, 2);
      String s = new String(input.readAllBytes());
      System.err.println("Got input");
      if (s.compareTo("OK") != 0) {
        System.out.println(s);
        return Status.ERROR;
      }

      sock.close();
    } catch (Exception e) {
      System.err.println("Error encountered for key: " + key + " " + e);
      //logger.error("Error encountered for key: " + key, e);
      return Status.ERROR;
    }

    return Status.OK;
  }

  protected static String createQualifiedKey(String table, String key) {
    return MessageFormat.format("{0}-{1}", table, key);
  }
}
