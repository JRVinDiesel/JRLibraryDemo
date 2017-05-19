package com.example.jrlibrary;

import android.app.Activity;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.PixelFormat;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.ref.WeakReference;

public class FileUtils {

	private static String tag = "FileUtils";


	public static boolean fileIsExists(String path) {
		try {
			File f = new File(path);
			if (!f.exists()) {
				return false;
			}
		} catch (Exception e) {

			return false;
		}
		return true;
	}

	/***
	 * 修改时间：2015年9月7日
	 * 作者：杜明悦
	 * 功能：bitmap转为byte[]
	 */
	public static byte[] bitmap2byte(Bitmap bitmap){
		ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.JPEG, 90, byteArrayOutputStream);
		return byteArrayOutputStream.toByteArray();
	}
	/**
	 * 修改时间：2015年10月30日
	 *
	 * 作者：张景瑞
	 *
	 * 功能：Drawable转换为Bitmap
	 */
	public static Bitmap drawableToBitmap(Drawable drawable){

		// 取drawable的长宽
		int width = drawable.getIntrinsicWidth();
		int height = drawable.getIntrinsicHeight();
		//取drawable的颜色格式
		Bitmap.Config config = drawable.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888: Bitmap.Config.RGB_565;
		// 建立对应bitmap
		Bitmap bitmap = Bitmap.createBitmap(width, height, config);
		// 建立对应bitmap的画布
		Canvas canvas = new Canvas(bitmap);
		drawable.setBounds(0, 0, width, height);
		// 把drawable内容画到画布中
		drawable.draw(canvas);
		return bitmap;

	}

	/**
	 * 修改时间：2015年9月1日
	 *
	 * 作者；张景瑞
	 *
	 * 功能：图片缩放
	 */
	public static void startPhotoZoom(Activity activity, int PHOTO_RESULT, Uri uri, int size){
		Intent intent = new Intent("com.android.camera.action.CROP");
		intent.setDataAndType(uri,"image/*");

		//crop为true是设置在开启的intent中设置显示的view可以裁剪
		intent.putExtra("crop", "true");

		//aspectX aspectY 是宽高的比例
		intent.putExtra("aspectX", 1);
		intent.putExtra("aspectY", 1);

		//outputX outputY 是裁剪图片宽高
		intent.putExtra("outputX", size);
		intent.putExtra("outputY", size);
		intent.putExtra("return-data", true);

		activity.startActivityForResult(intent, PHOTO_RESULT);
	}

	/***
	 * 修改时间：2015年9月7日
	 * 作者；杜明悦
	 * 功能：通过URI读取图片文件
	 * @param context 上下文
	 * @param uri 路径
	 * @return bitmap
	 */
	public static Bitmap getBitmapFromUri(Context context, Uri uri)
	{
		try
		{
			// 读取uri所在的图片
			Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), uri);
			return bitmap;
		}
		catch (Exception e)
		{
			Log.e("[Android]", e.getMessage());
			Log.e("[Android]", "目录为：" + uri);
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * 修改时间：2016年09月12日
	 *
	 * 作者：张景瑞
	 *
	 * 功能：根据uri拿到照片并按照指定比例缩放
	 * @param context
	 * @param uri
	 * @param scaleW
	 * @param scaleH
	 * @return
	 */
	public static Bitmap getBitmapFromUriWithScale(Context context, Uri uri, float scaleW, float scaleH) {
		Bitmap b = getBitmapFromUri(context,uri);
		Matrix matrix = new Matrix();
		matrix.postScale(scaleW,scaleH); //长和宽放大缩小的比例

		Bitmap bitmap = Bitmap.createBitmap(b,0,0,b.getWidth(),b.getHeight(),matrix,true);
		return bitmap;
	}

	/**
	 * 修改日期：2015年9月9日
	 * 作者：杜明悦
	 * 功能：从SDCard读取图片
	 * @param path 路径
	 * @param w 缩放后的宽度
	 * @param h 缩放后的高度
	 * @return bitmap
	 */
	public static Bitmap getBitmapForPath(String path, int w, int h) {
		BitmapFactory.Options opts = new BitmapFactory.Options();
		// 设置为ture只获取图片大小
		opts.inJustDecodeBounds = true;
		opts.inPreferredConfig = Bitmap.Config.ARGB_8888;

		// 返回为空
		BitmapFactory.decodeFile(path, opts);
		int width = opts.outWidth;
		int height = opts.outHeight;
		float scaleWidth = 0.f, scaleHeight = 0.f;
		if (width > w || height > h) {
			// 缩放
			scaleWidth = ((float) width) / w;
			scaleHeight = ((float) height) / h;
		}
		opts.inJustDecodeBounds = false;
		float scale = Math.max(scaleWidth, scaleHeight);
		opts.inSampleSize = (int)scale;
		WeakReference<Bitmap> weak = new WeakReference<Bitmap>(BitmapFactory.decodeFile(path, opts));
		Bitmap bitmap = Bitmap.createScaledBitmap(weak.get(), w, h, true);
		return adjustOritation(bitmap, path, true);
	}

	/**
	 * 修改时间：2015年9月10日
	 * 作者：杜明悦
	 * 功能：从给定的Bitmap图片，并指定是否自动旋转方向
	 * @param bitmap 旋转的图像
	 * @param imgpath 图片信息路径
	 * @param adjustOritation 是否自动旋转
	 * */
	public static Bitmap adjustOritation(Bitmap bitmap, String imgpath, boolean adjustOritation) {
		if (!adjustOritation) {
			return bitmap;
		} else {
			int digree = 0;
			ExifInterface exif = null;
			try {
				exif = new ExifInterface(imgpath);
			} catch (IOException e) {
				e.printStackTrace();
				exif = null;
			}
			if (exif != null) {
				// 读取图片中相机方向信息
				int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_UNDEFINED);
				// 计算旋转角度
				switch (ori) {
					case ExifInterface.ORIENTATION_ROTATE_90:
						digree = 90;
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						digree = 180;
						break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						digree = 270;
						break;
					default:
						digree = 0;
						break;
				}
			}
			if (digree != 0) {
				// 旋转图片
				Matrix m = new Matrix();
				m.postRotate(digree);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
						bitmap.getHeight(), m, true);
			}

			//写入原文件
			return bitmap;
		}
	}


	/**
	 * 修改时间：2015年9月10日
	 * 作者：杜明悦
	 * 功能：从给定的路径加载图片，并指定是否自动旋转方向
	 * @param imgpath 图片路径
	 * @param adjustOritation 是否自动旋转
	 * */
	public static Bitmap adjustOritation(String imgpath, boolean adjustOritation) {
		if (!adjustOritation) {
			return BitmapFactory.decodeFile(imgpath);
		} else {
			Bitmap bitmap = BitmapFactory.decodeFile(imgpath);
			int digree = 0;
			ExifInterface exif = null;
			try {
				exif = new ExifInterface(imgpath);
			} catch (IOException e) {
				e.printStackTrace();
				exif = null;
			}
			if (exif != null) {
				// 读取图片中相机方向信息
				int ori = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION,
						ExifInterface.ORIENTATION_UNDEFINED);
				// 计算旋转角度
				switch (ori) {
					case ExifInterface.ORIENTATION_ROTATE_90:
						digree = 90;
						break;
					case ExifInterface.ORIENTATION_ROTATE_180:
						digree = 180;
						break;
					case ExifInterface.ORIENTATION_ROTATE_270:
						digree = 270;
						break;
					default:
						digree = 0;
						break;
				}
			}
			if (digree != 0) {
				// 旋转图片
				Matrix m = new Matrix();
				m.postRotate(digree);
				bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(),
						bitmap.getHeight(), m, true);
			}

			//写入原文件
			return bitmap;
		}
	}



	/**
	 * 将URI转换为绝对路径
	 */
	public static String getImageAbsolutePath(Activity context, Uri imageUri) {
		if (context == null || imageUri == null)
			return null;
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.KITKAT && DocumentsContract.isDocumentUri(context, imageUri)) {
			if (isExternalStorageDocument(imageUri)) {
				String docId = DocumentsContract.getDocumentId(imageUri);
				String[] split = docId.split(":");
				String type = split[0];
				if ("primary".equalsIgnoreCase(type)) {
					return Environment.getExternalStorageDirectory() + "/" + split[1];
				}
			} else if (isDownloadsDocument(imageUri)) {
				String id = DocumentsContract.getDocumentId(imageUri);
				Uri contentUri = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
				return getDataColumn(context, contentUri, null, null);
			} else if (isMediaDocument(imageUri)) {
				String docId = DocumentsContract.getDocumentId(imageUri);
				String[] split = docId.split(":");
				String type = split[0];
				Uri contentUri = null;
				if ("image".equals(type)) {
					contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
				} else if ("video".equals(type)) {
					contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
				} else if ("audio".equals(type)) {
					contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
				}
				String selection = MediaStore.Images.Media._ID + "=?";
				String[] selectionArgs = new String[] { split[1] };
				return getDataColumn(context, contentUri, selection, selectionArgs);
			}
		} // MediaStore (and general)
		else if ("content".equalsIgnoreCase(imageUri.getScheme())) {
			// Return the remote address
			if (isGooglePhotosUri(imageUri))
				return imageUri.getLastPathSegment();
			return getDataColumn(context, imageUri, null, null);
		}
		// File
		else if ("file".equalsIgnoreCase(imageUri.getScheme())) {
			return imageUri.getPath();
		}
		return null;
	}
	public static String getDataColumn(Context context, Uri uri, String selection, String[] selectionArgs) {
		Cursor cursor  = null;
		String column = MediaStore.Images.Media.DATA;
		String[] projection = { column };
		try {
			cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs, null);
			if (cursor != null && cursor.moveToFirst()) {
				int index = cursor.getColumnIndexOrThrow(column);
				return cursor.getString(index);
			}
		} finally {
			if (cursor != null)
				cursor.close();
		}
		return null;
	}

	public static float getScreenDensity(Context context) {
		return context.getResources().getDisplayMetrics().density;
	}

	public static int dip2px(Context context, float px) {
		final float scale = getScreenDensity(context);
		return (int) (px * scale + 0.5);
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is ExternalStorageProvider.
	 */
	public static boolean isExternalStorageDocument(Uri uri) {
		return "com.android.externalstorage.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is DownloadsProvider.
	 */
	public static boolean isDownloadsDocument(Uri uri) {
		return "com.android.providers.downloads.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is MediaProvider.
	 */
	public static boolean isMediaDocument(Uri uri) {
		return "com.android.providers.media.documents".equals(uri.getAuthority());
	}

	/**
	 * @param uri The Uri to check.
	 * @return Whether the Uri authority is Google Photos.
	 */
	public static boolean isGooglePhotosUri(Uri uri) {
		return "com.google.android.apps.photos.content".equals(uri.getAuthority());
	}


}
