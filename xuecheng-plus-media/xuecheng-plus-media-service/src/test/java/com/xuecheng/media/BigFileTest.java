package com.xuecheng.media;


import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author CCL
 * @version 1.0
 * @description TODO
 * @createTime 2024-07-16 21:56
 **/

public class BigFileTest {

    //分块测试
    @Test
    public void testChunk() throws IOException {
        //先找到原文件
        File sourseFile = new File("D:\\迅雷下载\\beaf-083\\Bouncing and Cumming on my new Dildo - Pornhub.com.ts");
        //分块文件存储路径
        String chunkPath = "D:\\迅雷下载\\beaf-083\\chunk\\";
        //分块文件大小
        int chunkSize = 1024 * 1024 * 5;
        //分块文件的个数
        int chunkNum = (int) Math.ceil((double) sourseFile.length() / chunkSize);
        //使用流 从源文件读数据，向分块文件写数据
        RandomAccessFile raf_r = new RandomAccessFile(sourseFile, "r");

        //缓冲区
        byte[] bytes = new byte[1024];
        for (int i = 0; i < chunkNum; i++) {
            File chunkFile = new File(chunkPath + i);
            //分块文件写入流
            RandomAccessFile raf_rw = new RandomAccessFile(chunkFile, "rw");
            int len = -1;
            while ((len = raf_r.read(bytes)) != -1) {
                raf_rw.write(bytes, 0, len);
                if (chunkFile.length() >= chunkSize) {
                    break;
                }
            }
            raf_rw.close();
        }
        raf_r.close();

    }

    //合并测试
    @Test
    public void testMerge() throws IOException {

        //块文件目录
        File chunkFile = new File("D:\\迅雷下载\\beaf-083\\chunk");
        //先找到原文件
        File sourceFile = new File("D:\\迅雷下载\\beaf-083\\Bouncing and Cumming on my new Dildo - Pornhub.com.ts");
        //合并后的文件
        File mergeFile = new File("D:\\迅雷下载\\beaf-083\\result.ts");

        //取出所有分块文件
        File[] files = chunkFile.listFiles();
        //将数组转成list
        List<File> fileslist = Arrays.asList(files);

        Collections.sort(fileslist, new Comparator<File>() {
            @Override
            public int compare(File o1, File o2) {
                return Integer.parseInt(o1.getName()) - Integer.parseInt(o2.getName());
            }
        });
        //向合并文件写的流
        //遍历分块文件，向合并的文件写
        RandomAccessFile raf_rw = new RandomAccessFile(mergeFile, "rw");
        byte[] bytes = new byte[1024];
        for (File file : fileslist) {
            //读分块的流
            RandomAccessFile raf_r = new RandomAccessFile(file, "r");
            int len = -1;
            while ( (len=raf_r.read(bytes)) != -1){
                raf_rw.write(bytes,0,len);
            }
            raf_r.close();
        }
        raf_rw.close();

        //合并文件完成后对合并的文件校验
//        FileInputStream fileInputStream_merge = new FileInputStream(mergeFile);
//        FileInputStream fileInputStream_source = new FileInputStream(sourceFile);
//        String md5_merge = DigestUtils.md5Hex(fileInputStream_merge);
//        String md5_source = DigestUtils.md5Hex(fileInputStream_source);
        System.out.println(mergeFile.getTotalSpace());
        System.out.println(sourceFile.getTotalSpace());
        if (mergeFile.getTotalSpace() == sourceFile.getTotalSpace()){
            System.out.println("文件合成成功");
        }

    }
}