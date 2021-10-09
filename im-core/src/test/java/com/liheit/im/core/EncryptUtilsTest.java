package com.liheit.im.core;/*
package com.dx.im.core;

import com.dx.im.core.bean.Department;
import com.dx.im.core.bean.Result;
import com.dx.im.utils.AESUtils;
import com.fasterxml.jackson.core李四.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.stream.JsonReader;

import org.junit.Test;

import java.io.FileReader;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.IntUnaryOperator;

*/
/**
 * Created by daixun on 2018/6/14.
 *//*

public class EncryptUtilsTest {

    AtomicInteger index = new AtomicInteger(0);


    @Test
    public void decryptToString() throws Exception {

        String encStr=new String(AESUtils.encrypt("123"));
        System.out.println(encStr);
        String contnet = AESUtils.decryptToString(encStr);
        System.out.println(contnet);
    }

    //@Test
    public void encrypt() throws Exception {
        for (int x = 0; x < 5; x++) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 1000; i++) {
                        System.out.println(getIndex() + "   thread" + Thread.currentThread().getName());
                    }
                }
            }).start();
        }

        Thread.sleep(1000);

    }

    public int getIndex() {
        return index.getAndUpdate(new IntUnaryOperator() {
            @Override
            public int applyAsInt(int operand) {
                if (operand > 65535) {
                    return 0;
                } else {
                    return operand + 1;
                }
            }
        });
    }

    //    @Test
    public void testJackson() throws Exception {
        long beginTime = System.currentTimeMillis();
        final ObjectMapper mapper = new ObjectMapper();
        final JsonFactory factory = mapper.getFactory();

// Create a new streaming parser with the reader
        JsonParser jp = factory.createParser(new FileReader("/Users/daixun/Downloads/33F7ECBB493EE414943D869F58E991D1.json"));

        JsonToken token = jp.nextToken();


        while (jp.nextToken() != null) {

            switch (jp.currentToken()) {
                case FIELD_NAME:
                    if ("result".equals(jp.currentName())) {
                        System.out.println("result:" + jp.nextIntValue(0));
                    } else if ("t".equals(jp.currentName())) {
                        System.out.println("t:" + jp.nextIntValue(0));
                    } else if ("type".equals(jp.currentName())) {
                        System.out.println("type:" + jp.nextIntValue(0));
                    } else if ("depts".equals(jp.currentName())) {
                        jp.nextToken();
                        while (jp.nextToken() == JsonToken.START_OBJECT) {
                            Department d = jp.readValueAs(Department.class);
                            //System.out.println(d.toString());

                        }

                        */
/*System.out.println("type:" + jp.nextIntValue(0));*//*

                    }
                case START_ARRAY:
            }
            // Get customer POJO
            //final Customer c = mapper.readValue(jp, Customer.class);

            // Update local db with customer info.
            //myDataLayer.updateCustomer(c);
        }
        System.out.println("耗时：" + (System.currentTimeMillis() - beginTime));
    }

    //    @Test
    public void testGson() throws Exception {

        long beginTime = System.currentTimeMillis();
        JsonReader reader = new JsonReader(new FileReader("/Users/daixun/Downloads/33F7ECBB493EE414943D869F58E991D1.json"));

        Gson gson = new GsonBuilder().create();

        Result result = gson.fromJson(reader, Result.class);

        reader.beginObject();

        while (reader.hasNext()) {
            String name = reader.nextName();
            switch (name) {
                case "result":
                    System.out.println("result:" + reader.nextInt());
                    break;
                case "t":
                    System.out.println("t:" + reader.nextInt());
                    break;
                case "type":
                    System.out.println("type:" + reader.nextInt());
                    break;
                case "depts": {
                    reader.beginArray();
                    //reader.beginObject();
                    while (reader.hasNext()) {
                        Department dep = gson.fromJson(reader, Department.class);
                        //System.out.println(dep);
                    }
                    reader.endArray();
                    break;
                }
            }

        }
        reader.endObject();
        System.out.println("耗时：" + (System.currentTimeMillis() - beginTime));
    }

    //    @Test
    public void cc() throws Exception {
        testJackson();
        testGson();
    }

    //{"id":1001,"pid":1000,"cname":"碧桂园集团","ename":"碧桂园集团","t":1519047652,"type":1,"sort":1,"remark":"我的测试描述测试描述测试描述测试描述"}


}*/
