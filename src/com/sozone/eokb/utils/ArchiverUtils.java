/**
 * 包名：com.sozone.ebidkp.utils
 * 文件名：ArchiverUtils.java<br/>
 * 创建时间：2015-10-12 上午9:33:24<br/>
 * 创建者：yszy<br/>
 * 修改者：暂无<br/>
 * 修改简述：暂无<br/>
 * 修改详述：
 * <p>
 * 暂无<br/>
 * </p>
 * 修改时间：暂无<br/>
 */
package com.sozone.eokb.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.apache.tools.tar.TarEntry;
import org.apache.tools.tar.TarInputStream;
import org.apache.tools.tar.TarOutputStream;
import org.apache.tools.zip.ZipEntry;
import org.apache.tools.zip.ZipFile;
import org.apache.tools.zip.ZipOutputStream;

/**
 * 归档工具类定义<br/>
 * <p>
 * 该类实现了常用的归档及压缩方法,如:tar、zip、gz等<br/>
 * </p>
 * Time：2015-10-12 上午9:33:24<br/>
 * 
 * @author yszy
 * @version 1.0.0
 * @since 1.0.0
 */
public final class ArchiverUtils
{

	private static final char SPE = '/';

	/**
	 * 系统默认的字符集
	 */
	private static final String SYSTEM_DEFAULT_ENCODING = System
			.getProperty("sun.jnu.encoding");

	/**
	 * 私有构造函数
	 */
	private ArchiverUtils()
	{
	}

	/**
	 * 获取临时文件目录<br/>
	 * 
	 * @return 临时文件目录
	 * @throws IOException
	 *             IO异常
	 */
	public static File getTempDir() throws IOException
	{
		String tempdir = System.getProperty("java.io.tmpdir");
		if (null == tempdir || "".equals(tempdir))
		{
			throw new FileNotFoundException();
		}
		File dir = new File(tempdir);
		if (!dir.exists())
		{
			throw new FileNotFoundException();
		}
		return dir;
	}

	/**
	 * 使用tar算法对指定的文件数组进行归档<br/>
	 * 
	 * @param target
	 *            目标归档文件对象
	 * @param args
	 *            要归档的文件对象数组
	 * @throws IOException
	 *             IO异常
	 */
	public static void tar(File target, File... args) throws IOException
	{
		tar(target, SYSTEM_DEFAULT_ENCODING, args);
	}

	/**
	 * 使用tar算法对指定的文件数组进行归档<br/>
	 * 
	 * @param target
	 *            目标归档文件对象
	 * @param encoding
	 *            文件名的字符集
	 * @param args
	 *            要归档的文件对象数组
	 * @throws IOException
	 *             IO异常
	 */
	public static void tar(File target, String encoding, File... args)
			throws IOException
	{
		TarOutputStream out = null;
		BufferedOutputStream buffOut = null;
		FileOutputStream fout = null;
		try
		{
			fout = new FileOutputStream(target);
			buffOut = new BufferedOutputStream(fout);
			out = new TarOutputStream(buffOut, encoding);
			for (File src : args)
			{
				tar(src, out, "", true, 1024);
			}
			out.flush();
			buffOut.flush();
			fout.flush();
		}
		finally
		{
			if (null != out)
			{
				out.close();
			}
			if (null != buffOut)
			{
				buffOut.close();
			}
			if (null != fout)
			{
				fout.close();
			}
		}
	}

	/**
	 * 将指定目录的内容进行归档
	 * 
	 * @param src
	 *            目录
	 * @param out
	 *            归档输出流
	 * @param dir
	 *            相对目录
	 * @param isAllowEmptyDir
	 *            是否允许空目录归档
	 * @param blockSize
	 *            块大小
	 * @throws IOException
	 *             IO异常
	 */
	public static void tar(File src, TarOutputStream out, String dir,
			boolean isAllowEmptyDir, int blockSize) throws IOException
	{
		// 是目录
		if (src.isDirectory())
		{
			// 得出目录下所有的文件对象
			File[] listFile = src.listFiles();
			// 空目录归档
			if (listFile.length == 0 && isAllowEmptyDir)
			{
				// 将实体放入输出Tar流中
				out.putNextEntry(new TarEntry(dir + src.getName() + SPE));
				return;
			}
			/**
			 * 非空目录
			 */
			for (File cfile : listFile)
			{
				tar(cfile, out, dir + src.getName() + SPE, isAllowEmptyDir,
						blockSize);// 递归归档
			}

		}
		// 是文件
		if (src.isFile())
		{

			byte[] bt = new byte[blockSize];
			TarEntry ze = new TarEntry(dir + src.getName());// 构建tar实体
			// 设置压缩前的文件大小
			ze.setSize(src.length());
			// 将实体放入输出Tar流中
			out.putNextEntry(ze);
			FileInputStream fin = null;
			try
			{
				fin = new FileInputStream(src);
				int i = 0;
				while ((i = fin.read(bt)) != -1)
				{// 循环读出并写入输出Tar流中

					out.write(bt, 0, i);
				}
				out.flush();
			}
			finally
			{
				// 关闭输入流
				if (null != fin)
				{
					fin.close();
				}
				out.closeEntry();
			}
		}

	}

	/**
	 * 解压Tar包,默认缓存区大小为1024*2
	 * 
	 * @param src
	 *            要解压的tar包
	 * @param target
	 *            解压目录
	 * @throws IOException
	 *             IOException
	 */
	public static void unTar(File src, File target) throws IOException
	{
		unTar(src, target, SYSTEM_DEFAULT_ENCODING, 1024 * 2);
	}

	/**
	 * 解压Tar包
	 * 
	 * @param src
	 *            要解压的tar包
	 * @param target
	 *            解压目录
	 * @param encoding
	 *            文件字符集
	 * @param blockSize
	 *            缓冲区大小
	 * @throws IOException
	 *             IOException
	 */
	public static void unTar(File src, File target, String encoding,
			int blockSize) throws IOException
	{

		TarInputStream tarIn = null;
		FileInputStream fin = null;
		try
		{
			fin = new FileInputStream(src);
			tarIn = new TarInputStream(fin, blockSize, encoding);
			// 创建输出目录
			makeDir(target, null);
			TarEntry entry = null;
			File tmpFile = null;
			while ((entry = tarIn.getNextEntry()) != null)
			{
				// 是目录
				if (entry.isDirectory())
				{
					// 创建空目录
					makeDir(target, entry.getName());
					continue;
				}
				/**
				 * 是文件
				 */
				tmpFile = new File(target, entry.getName());
				// 创建输出目录
				makeDir(tmpFile.getParentFile(), null);
				OutputStream out = null;
				try
				{
					out = new FileOutputStream(tmpFile);
					int length = 0;
					byte[] b = new byte[blockSize];
					while ((length = tarIn.read(b)) != -1)
					{
						out.write(b, 0, length);
					}
					out.flush();
				}
				finally
				{
					if (out != null)
					{
						out.close();
					}
				}
			}
		}
		finally
		{
			if (null != fin)
			{
				fin.close();
			}
			if (null != tarIn)
			{
				tarIn.close();
			}

		}

	}

	/**
	 * 创建文件夹
	 * 
	 * @param parent
	 *            父目录
	 * @param subDirName
	 *            子目录名称
	 */
	private static void makeDir(File parent, String subDirName)
	{
		if (null != parent)
		{
			File dir = parent;
			if (null != subDirName && !"".equals(subDirName.trim()))
			{
				dir = new File(parent, subDirName);
			}
			if (!dir.exists())
			{
				// 当创建目录失败时，尝试创建多层目录
				if (!dir.mkdir())
				{
					dir.mkdirs();
				}
			}
		}
	}

	/**
	 * 创建Zip归档文件<br/>
	 * 
	 * @param target
	 *            zip归档文件对象
	 * @param args
	 *            需要归档的文件对象数组
	 * @throws IOException
	 *             IO异常
	 */
	public static void zip(File target, File... args) throws IOException
	{
		FileOutputStream fout = null;
		ZipOutputStream zout = null;
		try
		{
			fout = new FileOutputStream(target);
			zout = new ZipOutputStream(fout);
			zout.setEncoding(SYSTEM_DEFAULT_ENCODING);
			for (File src : args)
			{
				zip(src, zout, "", 1024, target.getCanonicalPath());
			}
			zout.flush();
			fout.flush();
		}
		finally
		{
			if (null != zout)
			{
				zout.close();
			}
			if (null != fout)
			{
				fout.close();
			}
		}
	}

	/**
	 * 打jar包
	 * 
	 * @param src
	 *            要打包的文件
	 * @param zout
	 *            ZipOutputStream
	 * @param relativePath
	 *            相对jar包中的父目录的相对路径
	 * @param bufferSize
	 *            缓冲区大小
	 * @param target
	 *            目标zip文件全路径
	 * @throws IOException
	 *             IOException
	 */
	public static void zip(File src, ZipOutputStream zout, String relativePath,
			int bufferSize, String target) throws IOException
	{
		if (src.isDirectory())
		{
			File[] flist = src.listFiles();
			String subPath = (relativePath == null) ? "" : (relativePath
					+ src.getName() + SPE);
			if (relativePath != null)
			{
				ZipEntry zentry = new ZipEntry(subPath);
				zentry.setTime(src.lastModified());
				zout.putNextEntry(zentry);
				zout.flush();
				zout.closeEntry();
			}
			for (File file : flist)
			{
				zip(file, zout, subPath, bufferSize, target);
			}
		}
		else
		{
			if (src.getCanonicalPath().equals(target))
			{
				return;
			}
			FileInputStream fin = null;
			try
			{
				fin = new FileInputStream(src);
				ZipEntry entry = new ZipEntry(relativePath + src.getName());
				entry.setTime(src.lastModified());
				int index = 0;
				byte[] buffer = new byte[bufferSize];
				zout.putNextEntry(entry);
				while ((index = fin.read(buffer)) != -1)
				{
					zout.write(buffer, 0, index);
				}
				zout.flush();
				zout.closeEntry();
			}
			finally
			{
				if (null != fin)
				{
					fin.close();
				}
			}
		}
	}

	/**
	 * 解压Zip归档文件<br/>
	 * 
	 * @param src
	 *            zip文件对象
	 * @param targetDir
	 *            目标目录
	 * @throws IOException
	 *             IO异常
	 */
	public static void unZip(File src, File targetDir) throws IOException
	{
		unZip(src, SYSTEM_DEFAULT_ENCODING, targetDir);
	}

	/**
	 * 解压Zip归档文件<br/>
	 * 
	 * @param src
	 *            zip文件对象
	 * @param encoding
	 *            zip文件字符集
	 * @param targetDir
	 *            目标目录
	 * @throws IOException
	 *             IO异常
	 */
	public static void unZip(File src, String encoding, File targetDir)
			throws IOException
	{
		unZip(new ZipFile(src, encoding), targetDir, 1024 * 2);
	}

	/**
	 * 解压Zip归档文件<br/>
	 * 
	 * @param src
	 *            ZipFile对象
	 * @param targetDir
	 *            目标目录
	 * @param bufferSize
	 *            缓冲区大小
	 * @throws IOException
	 *             IO异常
	 */
	public static void unZip(ZipFile src, File targetDir, int bufferSize)
			throws IOException
	{
		Enumeration<ZipEntry> emu = src.getEntries();
		FileOutputStream fout = null;
		BufferedOutputStream bout = null;
		BufferedInputStream bin = null;
		while (emu.hasMoreElements())
		{
			ZipEntry entry = emu.nextElement();
			if (entry.isDirectory())
			{
				File dir = new File(targetDir, entry.getName());
				if (!dir.exists())
				{
					dir.mkdirs();
				}
				continue;
			}
			try
			{
				bin = new BufferedInputStream(src.getInputStream(entry));

				File file = new File(targetDir, entry.getName());
				File parent = file.getParentFile();
				if (parent != null && (!parent.exists()))
				{
					parent.mkdirs();
				}
				fout = new FileOutputStream(file);
				bout = new BufferedOutputStream(fout, bufferSize);
				byte[] buf = new byte[bufferSize];
				int len = 0;
				while ((len = bin.read(buf, 0, bufferSize)) != -1)
				{
					bout.write(buf, 0, len);
				}
				bout.flush();
				fout.flush();
			}
			finally
			{
				if (bin != null)
				{
					bin.close();
				}
				if (bout != null)
				{
					bout.close();
				}
				if (fout != null)
				{
					fout.close();
				}
			}
		}
		src.close();
	}

	/**
	 * 使用gzip方式压缩文件<br/>
	 * 
	 * @param src
	 *            源文件
	 * @param target
	 *            目标文件
	 * @throws IOException
	 *             IO异常
	 */
	public static void gzip(File src, File target) throws IOException
	{
		gzip(src, target, 1204 * 2);
	}

	/**
	 * 使用gzip方式压缩文件<br/>
	 * 
	 * @param src
	 *            源文件
	 * @param target
	 *            目标文件
	 * @param bufferSize
	 *            缓冲区大小
	 * @throws IOException
	 *             IO异常
	 */
	public static void gzip(File src, File target, int bufferSize)
			throws IOException
	{
		FileInputStream fin = null;
		FileOutputStream fout = null;
		GZIPOutputStream gzout = null;
		try
		{
			fin = new FileInputStream(src);
			fout = new FileOutputStream(target);
			gzout = new GZIPOutputStream(fout);
			byte[] buf = new byte[bufferSize];
			int num;
			while ((num = fin.read(buf)) != -1)
			{
				gzout.write(buf, 0, num);
			}
			gzout.flush();
			fout.flush();
		}
		finally
		{
			if (gzout != null)
			{
				gzout.close();
			}
			if (fout != null)
			{
				fout.close();
			}
			if (fin != null)
			{
				fin.close();
			}
		}
	}

	/**
	 * 解压gzip格式文件
	 * 
	 * @param source
	 *            源文件
	 * @param target
	 *            目标文件
	 * @throws IOException
	 *             IO异常
	 */
	public static void unGzip(File source, File target) throws IOException
	{
		unGzip(source, target, 1024 * 2);
	}

	/**
	 * 解压gzip格式文件
	 * 
	 * @param source
	 *            源文件
	 * @param target
	 *            目标文件
	 * @param bufferSize
	 *            缓冲区大小
	 * @throws IOException
	 *             IO异常
	 */
	public static void unGzip(File source, File target, int bufferSize)
			throws IOException
	{
		FileInputStream fin = null;
		GZIPInputStream gzin = null;
		FileOutputStream fout = null;
		try
		{
			fin = new FileInputStream(source);
			gzin = new GZIPInputStream(fin);
			fout = new FileOutputStream(target);
			byte[] buf = new byte[bufferSize];
			int num;
			while ((num = gzin.read(buf, 0, buf.length)) != -1)
			{
				fout.write(buf, 0, num);
			}
			fout.flush();
		}
		finally
		{
			if (fout != null)
			{
				fout.close();
			}
			if (gzin != null)
			{
				gzin.close();
			}
			if (fin != null)
			{
				fin.close();
			}
		}
	}

}
