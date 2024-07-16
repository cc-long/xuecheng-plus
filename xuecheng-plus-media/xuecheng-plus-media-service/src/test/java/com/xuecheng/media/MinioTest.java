package com.xuecheng.media;

import com.j256.simplemagic.ContentInfo;
import com.j256.simplemagic.ContentInfoUtil;
import io.minio.*;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import java.io.*;

/**
 * @author CCL
 * @version 1.0
 * @description 测试minIO 的sdk
 * @createTime 2024-07-13 18:21
 **/
public class MinioTest {
    MinioClient minioClient =
            MinioClient.builder()
                    .endpoint("http://192.168.61.139:9000")
                    .credentials("minioadmin", "minioadmin")
                    .build();

    @Test
    public void test_upload() throws Exception {

        //通过扩展名得到媒体资源类型mediaTYPE
        //更具扩展名取出mediatype
        ContentInfo extensionMatch = ContentInfoUtil.findExtensionMatch(".jpg");
        String mimeType = MediaType.APPLICATION_OCTET_STREAM_VALUE;
        if (extensionMatch != null) {
            mimeType = extensionMatch.getMimeType();
        }
        minioClient.uploadObject(
                UploadObjectArgs.builder()
                        .bucket("testbucket")
                        //指定本地文件
                        .filename("C:\\Users\\chaic\\OneDrive\\图片\\荧妹.jpg")
                        //确定对象名
                        .object("荧妹.jpg")
                        .contentType(mimeType)
                        .build()
        );
    }

    @Test
    public void test_remove() throws Exception {

        minioClient.removeObject(
                RemoveObjectArgs.builder()
                        .bucket("testbucket")
                        //确定对象名
                        .object("荧妹.jpg")
                        .build()
        );
    }

    @Test
    public void test_getFile() throws Exception {

        GetObjectArgs testbucket = GetObjectArgs.builder()
                .bucket("testbucket")
                .object("荧妹.jpg")
                .build();

        GetObjectResponse inputStream = minioClient.getObject(testbucket);
        File file = new File("C:\\Users\\chaic\\OneDrive\\图片\\荧妹.jpg");
        if (file.exists()){
            file.delete();
        }
        //指定输出流
        FileOutputStream outputStream = new FileOutputStream(file);
        IOUtils.copy(inputStream,outputStream);

        //校验文件的 完整性 对文件的内容进行md5
        //md5 摘要算法
        String source_md5 = DigestUtils.md5Hex(inputStream);    //minio中文件的md5
        String local_md5 = DigestUtils.md5Hex(new FileInputStream(file));
        if (source_md5.equals(local_md5)){
            System.out.println("成功");
        }else System.out.println("文件不完整");
    }

    @Test
    public void test_download() throws Exception {

        minioClient.downloadObject(DownloadObjectArgs.builder()
                        .bucket("testbucket")
                        .filename("C:\\Users\\chaic\\OneDrive\\图片\\荧妹.jpg")
                        .object("荧妹.jpg")
                .build()
        );
    }

    @Test
    public void upload_chunk() throws Exception {

        for (int i=0;i<27;i++) {
            minioClient.uploadObject(
                    UploadObjectArgs.builder()
                            .bucket("testbucket")
                            //指定本地文件
                            .filename("D:\\迅雷下载\\beaf-083\\chunk\\" + i)
                            //确定对象名
                            .object("chunk/" + i)
                            .build()
            );
            System.out.println("上传分块" + i + "成功");
        }
    }
}
