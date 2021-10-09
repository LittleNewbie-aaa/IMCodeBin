package com.liheit.im.utils;

import android.util.Log;

import net.lingala.zip4j.exception.ZipException;
import net.lingala.zip4j.io.ZipInputStream;
import net.lingala.zip4j.model.FileHeader;
import net.lingala.zip4j.model.ZipParameters;
import net.lingala.zip4j.util.Zip4jConstants;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import okio.BufferedSink;
import okio.BufferedSource;
import okio.Okio;

/**
 * <pre>
 *     author: Blankj
 *     blog  : http://blankj.com
 *     time  : 2016/08/27
 *     desc  : utils about zip
 * </pre>
 */
public final class ZipUtils {

    private static final int BUFFER_LEN = 8192;

    private ZipUtils() {
        throw new UnsupportedOperationException("u can't instantiate me...");
    }

    /**
     * Zip the files.
     *
     * @param srcFiles    The source of files.
     * @param zipFilePath The path of ZIP file.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFiles(final Collection<String> srcFiles,
                                   final String zipFilePath)
            throws IOException {
        return zipFiles(srcFiles, zipFilePath, null);
    }

    /**
     * Zip the files.
     *
     * @param srcFilePaths The paths of source files.
     * @param zipFilePath  The path of ZIP file.
     * @param comment      The comment.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFiles(final Collection<String> srcFilePaths,
                                   final String zipFilePath,
                                   final String comment)
            throws IOException {
        if (srcFilePaths == null || zipFilePath == null) return false;
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFilePath));
            for (String srcFile : srcFilePaths) {
                if (!zipFile(getFileByPath(srcFile), "", zos, comment)) return false;
            }
            return true;
        } finally {
            if (zos != null) {
                zos.finish();
                zos.close();
            }
        }
    }

    /**
     * Zip the files.
     *
     * @param srcFiles The source of files.
     * @param zipFile  The ZIP file.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFiles(final Collection<File> srcFiles, final File zipFile)
            throws IOException {
        return zipFiles(srcFiles, zipFile, null);
    }

    /**
     * Zip the files.
     *
     * @param srcFiles The source of files.
     * @param zipFile  The ZIP file.
     * @param comment  The comment.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFiles(final Collection<File> srcFiles,
                                   final File zipFile,
                                   final String comment)
            throws IOException {
        if (srcFiles == null || zipFile == null) return false;
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            for (File srcFile : srcFiles) {
                if (!zipFile(srcFile, "", zos, comment)) return false;
            }
            return true;
        } finally {
            if (zos != null) {
                zos.finish();
                zos.close();
            }
        }
    }

    /**
     * Zip the file.
     *
     * @param srcFilePath The path of source file.
     * @param zipFilePath The path of ZIP file.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFile(final String srcFilePath,
                                  final String zipFilePath)
            throws IOException {
        return zipFile(getFileByPath(srcFilePath), getFileByPath(zipFilePath), null);
    }

    /**
     * Zip the file.
     *
     * @param srcFilePath The path of source file.
     * @param zipFilePath The path of ZIP file.
     * @param comment     The comment.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFile(final String srcFilePath,
                                  final String zipFilePath,
                                  final String comment)
            throws IOException {
        return zipFile(getFileByPath(srcFilePath), getFileByPath(zipFilePath), comment);
    }

    /**
     * Zip the file.
     *
     * @param srcFile The source of file.
     * @param zipFile The ZIP file.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFile(final File srcFile,
                                  final File zipFile)
            throws IOException {
        return zipFile(srcFile, zipFile, null);
    }

    /**
     * Zip the file.
     *
     * @param srcFile The source of file.
     * @param zipFile The ZIP file.
     * @param comment The comment.
     * @return {@code true}: success<br>{@code false}: fail
     * @throws IOException if an I/O error has occurred
     */
    public static boolean zipFile(final File srcFile,
                                  final File zipFile,
                                  final String comment)
            throws IOException {
        if (srcFile == null || zipFile == null) return false;
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(zipFile));
            return zipFile(srcFile, "", zos, comment);
        } finally {
            if (zos != null) {
                zos.close();
            }
        }
    }

    private static boolean zipFile(final File srcFile,
                                   String rootPath,
                                   final ZipOutputStream zos,
                                   final String comment)
            throws IOException {
        rootPath = rootPath + (isSpace(rootPath) ? "" : File.separator) + srcFile.getName();
        if (srcFile.isDirectory()) {
            File[] fileList = srcFile.listFiles();
            if (fileList == null || fileList.length <= 0) {
                ZipEntry entry = new ZipEntry(rootPath + '/');
                entry.setComment(comment);
                zos.putNextEntry(entry);
                zos.closeEntry();
            } else {
                for (File file : fileList) {
                    if (!zipFile(file, rootPath, zos, comment)) return false;
                }
            }
        } else {
            InputStream is = null;
            try {
                is = new BufferedInputStream(new FileInputStream(srcFile));
                ZipEntry entry = new ZipEntry(rootPath);
                entry.setComment(comment);
                zos.putNextEntry(entry);
                byte buffer[] = new byte[BUFFER_LEN];
                int len;
                while ((len = is.read(buffer, 0, BUFFER_LEN)) != -1) {
                    zos.write(buffer, 0, len);
                }
                zos.closeEntry();
            } finally {
                if (is != null) {
                    is.close();
                }
            }
        }
        return true;
    }

    /**
     * Unzip the file.
     *
     * @param zipFilePath The path of ZIP file.
     * @param destDirPath The path of destination directory.
     * @return the unzipped files
     * @throws IOException if unzip unsuccessfully
     */
    public static List<File> unzipFile(final String zipFilePath,
                                       final String destDirPath)
            throws IOException {
        return unzipFileByKeyword(zipFilePath, destDirPath, null);
    }

    /**
     * Unzip the file.
     *
     * @param zipFile The ZIP file.
     * @param destDir The destination directory.
     * @return the unzipped files
     * @throws IOException if unzip unsuccessfully
     */
    public static List<File> unzipFile(final File zipFile,
                                       final File destDir)
            throws IOException {
        return unzipFileByKeyword(zipFile, destDir, null);
    }

    /**
     * Unzip the file by keyword.
     *
     * @param zipFilePath The path of ZIP file.
     * @param destDirPath The path of destination directory.
     * @param keyword     The keyboard.
     * @return the unzipped files
     * @throws IOException if unzip unsuccessfully
     */
    public static List<File> unzipFileByKeyword(final String zipFilePath,
                                                final String destDirPath,
                                                final String keyword)
            throws IOException {
        return unzipFileByKeyword(getFileByPath(zipFilePath), getFileByPath(destDirPath), keyword);
    }

    /**
     * Unzip the file by keyword.
     *
     * @param zipFile The ZIP file.
     * @param destDir The destination directory.
     * @param keyword The keyboard.
     * @return the unzipped files
     * @throws IOException if unzip unsuccessfully
     */
    public static List<File> unzipFileByKeyword(final File zipFile,
                                                final File destDir,
                                                final String keyword)
            throws IOException {
        if (zipFile == null || destDir == null) return null;
        List<File> files = new ArrayList<>();
        ZipFile zf = new ZipFile(zipFile);
        Enumeration<?> entries = zf.entries();
        if (isSpace(keyword)) {
            while (entries.hasMoreElements()) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                String entryName = entry.getName();
                if (entryName.contains("../")) {
                    Log.e("ZipUtils", "it's dangerous!");
                    return files;
                }
                if (!unzipChildFile(destDir, files, zf, entry, entryName)) return files;
            }
        } else {
            while (entries.hasMoreElements()) {
                ZipEntry entry = ((ZipEntry) entries.nextElement());
                String entryName = entry.getName();
                if (entryName.contains("../")) {
                    Log.e("ZipUtils", "it's dangerous!");
                    return files;
                }
                if (entryName.contains(keyword)) {
                    if (!unzipChildFile(destDir, files, zf, entry, entryName)) return files;
                }
            }
        }
        return files;
    }

    private static boolean unzipChildFile(final File destDir,
                                          final List<File> files,
                                          final ZipFile zf,
                                          final ZipEntry entry,
                                          final String entryName) throws IOException {
        String filePath = destDir + File.separator + entryName;
        File file = new File(filePath);
        files.add(file);
        if (entry.isDirectory()) {
            if (!createOrExistsDir(file)) return false;
        } else {
            if (!createOrExistsFile(file)) return false;
            InputStream in = null;
            OutputStream out = null;
            try {
                in = new BufferedInputStream(zf.getInputStream(entry));
                out = new BufferedOutputStream(new FileOutputStream(file));
                byte buffer[] = new byte[BUFFER_LEN];
                int len;
                while ((len = in.read(buffer)) != -1) {
                    out.write(buffer, 0, len);
                }
            } finally {
                if (in != null) {
                    in.close();
                }
                if (out != null) {
                    out.close();
                }
            }
        }
        return true;
    }

    /**
     * Return the files' path in ZIP file.
     *
     * @param zipFilePath The path of ZIP file.
     * @return the files' path in ZIP file
     * @throws IOException if an I/O error has occurred
     */
    public static List<String> getFilesPath(final String zipFilePath)
            throws IOException {
        return getFilesPath(getFileByPath(zipFilePath));
    }

    /**
     * Return the files' path in ZIP file.
     *
     * @param zipFile The ZIP file.
     * @return the files' path in ZIP file
     * @throws IOException if an I/O error has occurred
     */
    public static List<String> getFilesPath(final File zipFile)
            throws IOException {
        if (zipFile == null) return null;
        List<String> paths = new ArrayList<>();
        Enumeration<?> entries = new ZipFile(zipFile).entries();
        while (entries.hasMoreElements()) {
            paths.add(((ZipEntry) entries.nextElement()).getName());
        }
        return paths;
    }

    /**
     * Return the files' comment in ZIP file.
     *
     * @param zipFilePath The path of ZIP file.
     * @return the files' comment in ZIP file
     * @throws IOException if an I/O error has occurred
     */
    public static List<String> getComments(final String zipFilePath)
            throws IOException {
        return getComments(getFileByPath(zipFilePath));
    }

    /**
     * Return the files' comment in ZIP file.
     *
     * @param zipFile The ZIP file.
     * @return the files' comment in ZIP file
     * @throws IOException if an I/O error has occurred
     */
    public static List<String> getComments(final File zipFile)
            throws IOException {
        if (zipFile == null) return null;
        List<String> comments = new ArrayList<>();
        Enumeration<?> entries = new ZipFile(zipFile).entries();
        while (entries.hasMoreElements()) {
            ZipEntry entry = ((ZipEntry) entries.nextElement());
            comments.add(entry.getComment());
        }
        return comments;
    }

    private static boolean createOrExistsDir(final File file) {
        return file != null && (file.exists() ? file.isDirectory() : file.mkdirs());
    }

    private static boolean createOrExistsFile(final File file) {
        if (file == null) return false;
        if (file.exists()) return file.isFile();
        if (!createOrExistsDir(file.getParentFile())) return false;
        try {
            return file.createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private static File getFileByPath(final String filePath) {
        return isSpace(filePath) ? null : new File(filePath);
    }

    private static boolean isSpace(final String s) {
        if (s == null) return true;
        for (int i = 0, len = s.length(); i < len; ++i) {
            if (!Character.isWhitespace(s.charAt(i))) {
                return false;
            }
        }
        return true;
    }


    public static File zip(String srcFile, String dest, String password) throws ZipException {
        File srcfile = new File(srcFile);
        //创建目标文件
        ZipParameters par = new ZipParameters();
        par.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        par.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        if (password != null) {
            par.setEncryptFiles(true);
            par.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            par.setPassword(password.toCharArray());
        }
        net.lingala.zip4j.core.ZipFile zipfile = new net.lingala.zip4j.core.ZipFile(dest);
        try {
            if (srcfile.isDirectory()) {
                zipfile.addFolder(srcfile, par);
            } else {
                zipfile.addFile(srcfile, par);
            }
            return zipfile.getFile();
        } catch (ZipException e) {
            e.printStackTrace();
            return null;
        }

    }

    public static void zip(String zipContentStr, String fileName, File outputFile, String password) throws ZipException, IOException {
        //创建目标文件
        ZipParameters par = new ZipParameters();
        par.setCompressionMethod(Zip4jConstants.COMP_DEFLATE);
        par.setCompressionLevel(Zip4jConstants.DEFLATE_LEVEL_NORMAL);
        if (password != null) {
            par.setEncryptFiles(true);
            par.setEncryptionMethod(Zip4jConstants.ENC_METHOD_STANDARD);
            par.setPassword(password.toCharArray());
        }
        if (outputFile.exists()) {
            outputFile.delete();
        }
        File temp = new File(outputFile.getParent(), fileName);
        BufferedSink buffer = Okio.buffer(Okio.sink(temp));
        buffer.writeUtf8(zipContentStr);
        buffer.flush();
        buffer.close();

        net.lingala.zip4j.core.ZipFile zipfile = new net.lingala.zip4j.core.ZipFile(outputFile);
        zipfile.createZipFile(temp, par);
    }

    public static void unzip(String filePath, File outputDir, String password) throws ZipException, IOException {
        net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(filePath);
        if (zipFile.isEncrypted()) {
            zipFile.setPassword(password);
        }
        List<FileHeader> fileHeaderList = zipFile.getFileHeaders();
        for (int i = 0; i < fileHeaderList.size(); i++) {
            FileHeader fileHeader = fileHeaderList.get(i);
            ZipInputStream inputStream = zipFile.getInputStream(fileHeader);
            File destFile = new File(outputDir.getAbsolutePath(), fileHeader.getFileName());
            BufferedSource source = Okio.buffer(Okio.source(inputStream));
            BufferedSink sink = Okio.buffer(Okio.sink(destFile));
            sink.writeAll(source);
            sink.close();
            source.close();
//            zipFile.extractFile(fileHeader, new File(outputDir.getAbsolutePath(),fileHeader.getFileName()).getAbsolutePath(),);
        }
    }

    public static String readZip(String filePath, String password) throws ZipException, IOException {
        net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(filePath);
        if (zipFile.isEncrypted()) {
            zipFile.setPassword(password);
        }
        List<FileHeader> fileHeaderList = zipFile.getFileHeaders();
        for (int i = 0; i < fileHeaderList.size(); i++) {
            FileHeader fileHeader = fileHeaderList.get(i);
            if (!fileHeader.isDirectory()) {
                ZipInputStream inputStream = zipFile.getInputStream(fileHeader);
                BufferedSource source = Okio.buffer(Okio.source(inputStream));
                String content = source.readUtf8(fileHeader.getUncompressedSize());

                inputStream.close();

                return content;
            }
        }
        return null;
    }

    public static String chatRecoverReadZip(String filePath, String password) throws ZipException, IOException {
        net.lingala.zip4j.core.ZipFile zipFile = new net.lingala.zip4j.core.ZipFile(filePath);
        if (!zipFile.isValidZipFile()) {
            return "文件错误";
        }
        if (zipFile.isEncrypted()) {
            zipFile.setPassword(password);
        } else {
            return "文件错误";
        }
        try {
            List<FileHeader> fileHeaderList = zipFile.getFileHeaders();
            for (int i = 0; i < fileHeaderList.size(); i++) {
                FileHeader fileHeader = fileHeaderList.get(i);
                if (!fileHeader.isDirectory()) {
                    ZipInputStream inputStream = zipFile.getInputStream(fileHeader);
                    BufferedSource source = Okio.buffer(Okio.source(inputStream));
                    String content = source.readUtf8(fileHeader.getUncompressedSize());
                    inputStream.close();
                    return content;

                }
            }
        } catch (ZipException | IOException exception) {
            return "密码错误";
        }
        return null;
    }

    public static String readZip(File tempFile, byte[] bytes) {
        try {
            BufferedSink sink = Okio.buffer(Okio.sink(tempFile));
            sink.write(bytes, 0, bytes.length - 16);
            sink.close();

            byte[] dics = new byte[16];
            System.arraycopy(bytes, bytes.length - 16, dics, 0, 16);
            String pwd = calculateZipPwd(dics);
            String content = readZip(tempFile.getAbsolutePath(), pwd);
            tempFile.delete();
            return content;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String calculateZipPwd(String pwd) {
        return calculateZipPwd(pwd.getBytes());
    }

    public static String calculateZipPwd(byte[] pszDict) {
        int nLen = pszDict.length;
        for (int x = 0; x < pszDict.length; x++) {
            if (pszDict[x] == 0) {
                nLen = x;
                break;
            }
        }

//        int nLen=pszDict.length;
        byte[] aszPsw = new byte[nLen];
        int nCrc = 0;
        for (int i = 0; i < nLen; i++) {    // 使用移位累加计算CRC
            nCrc <<= 1;            // 左移一位
            nCrc |= pszDict[i];    // 或上字典值
        }
        for (int n = 0; n < nLen; n++) {    // 生成zip密码
            if (((nCrc >> n) & 1) == 1)
                aszPsw[n] = pszDict[(n << 1) % nLen];
            else
                aszPsw[n] = pszDict[((n + 1) * (n + 3)) % nLen];
        }
        return new String(aszPsw, Charset.forName("UTF-8"));
    }

    public static boolean fileToZips(String sourceFilePath, String zipFilePath, String zipFileName) throws Exception {
        boolean flag = false;
        BufferedInputStream bis = null;
        ZipOutputStream zos = null;
        File sourceFiles = new File(sourceFilePath);
        if (sourceFiles.exists() == false) {
            com.liheit.im.utils.Log.e("待压缩目录" + sourceFiles + "不存在...");
        } else {
            //判断待压缩文件是否已经被压缩过 zipFiles--->fos--->zos设置压缩包名称
            File zipFiles = new File(zipFilePath + "/" + zipFileName + ".zip");
            zipFiles.getParentFile().mkdirs();
            if (zipFiles.exists()) {
                com.liheit.im.utils.Log.e(zipFilePath + "目录下已经存在" + zipFileName + ".zip" + "压缩包...");
            } else {
                //得到指定目录下的所有文件夹，注意返回的File数组，不是String数组
                File[] sourceFileList = sourceFiles.listFiles();
                if (null == sourceFileList || sourceFileList.length < 1) {
                    com.liheit.im.utils.Log.e(sourceFilePath + "目录下没有待压缩的文件...");
                } else {
                    FileOutputStream fos = new FileOutputStream(zipFiles);
                    zos = new ZipOutputStream(fos);
                    byte[] bys = new byte[1024 * 10];
                    for (int i = 0; i < sourceFileList.length; i++) {
                        //fileName：指定目录下的文件夹的名称或文件的名称
                        String fileName = sourceFileList[i].getName();
                        //创建zip实体，并添加进压缩包  ，zipEntry的值就是fileName的值
                        ZipEntry zipEntry = new ZipEntry(sourceFileList[i].getName());
                        //添加进压缩包
                        zos.putNextEntry(zipEntry);
                        //读取待压缩文件，并添加进压缩包，FileInputStream读的是一个具体文件，不是一个文件夹
                        FileInputStream fis = new FileInputStream(sourceFileList[i]);
                        //如果遇到bis存储空间不够，可以把1024*10的限制取消。
                        bis = new BufferedInputStream(fis, 1024 * 10);
                        //记录当前读取的字节数
                        int reads = 0;
                        while ((reads = bis.read(bys, 0, 1024 * 10)) != -1) {
                            zos.write(bys, 0, reads);
                        }
                    }
                    flag = true;
                    //特别注意，如果不关闭流端口，可能会导致压缩打包不成功！
                    if (null != bis) {
                        bis.close();
                    }
                    if (null != zos) {
                        zos.close();
                    }
                }
            }
        }
        return flag;
    }
}