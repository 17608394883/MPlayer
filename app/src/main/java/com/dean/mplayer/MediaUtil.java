package com.dean.mplayer;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.VectorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.content.ContextCompat;

public class MediaUtil {

	//版本判断
	public static boolean isOreo() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
	}
	public static boolean isMarshmallow() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
	}
	public static boolean isLollipop() {
		return Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;
	}

	//歌曲信息获取
	public static List<MusicInfo> getMusicInfos(Context context) {
		Cursor cursor = context.getContentResolver().query(
				MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, null, null, null,
				MediaStore.Audio.Media.DEFAULT_SORT_ORDER);

		List<MusicInfo> mp3Infos = new ArrayList<>();
		for (int i = 0; i < cursor.getCount(); i++) {
			cursor.moveToNext();
			MusicInfo mp3Info = new MusicInfo();
			long id = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media._ID));
			String title = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.TITLE));
			String artist = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ARTIST));
			String album = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.ALBUM));
			String displayName = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
			long albumId = cursor.getInt(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID));
			long duration = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media.DURATION));
			long size = cursor.getLong(cursor
					.getColumnIndex(MediaStore.Audio.Media.SIZE));
			String url = cursor.getString(cursor
					.getColumnIndex(MediaStore.Audio.Media.DATA));
			Uri uri = Uri.withAppendedPath(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, String.valueOf(id));
			int isMusic = cursor.getInt(cursor
					.getColumnIndex(MediaStore.Audio.Media.IS_MUSIC));
			if (isMusic != 0) {
				mp3Info.setId(id);
				mp3Info.setTitle(title);
				mp3Info.setArtist(artist);
				mp3Info.setAlbum(album);
				mp3Info.setDisplayName(displayName);
				mp3Info.setAlbumId(albumId);
				mp3Info.setDuration(duration);
				mp3Info.setSize(size);
				mp3Info.setUrl(url);
				mp3Info.setUri(uri);
				mp3Infos.add(mp3Info);
			}
		}
		return mp3Infos;
	}

	public static List<HashMap<String, String>> getMusicMaps(
			List<MusicInfo> mp3Infos) {
		List<HashMap<String, String>> musiclist = new ArrayList<>();
		for (Iterator iterator = mp3Infos.iterator(); iterator.hasNext();) {
			MusicInfo mp3Info = (MusicInfo) iterator.next();
			HashMap<String, String> map = new HashMap<>();
			map.put("title", mp3Info.getTitle());
			map.put("artist", mp3Info.getArtist());
			map.put("album", mp3Info.getAlbum());
			map.put("displayName", mp3Info.getDisplayName());
			map.put("albumId", String.valueOf(mp3Info.getAlbumId()));
			map.put("duration", formatTime(mp3Info.getDuration()));
			map.put("size", String.valueOf(mp3Info.getSize()));
			map.put("url", mp3Info.getUrl());
			map.put("uri",String.valueOf(mp3Info.getUri()));
			musiclist.add(map);
		}
		return musiclist;
	}

	//时间显示格式
	public static String formatTime(long time) {
		String min = time / (1000 * 60) + "";
		String sec = time % (1000 * 60) + "";
		if (min.length() < 2) {
			min = "0" + time / (1000 * 60) + "";
		} else {
			min = time / (1000 * 60) + "";
		}
		if (sec.length() == 4) {
			sec = "0" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 3) {
			sec = "00" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 2) {
			sec = "000" + (time % (1000 * 60)) + "";
		} else if (sec.length() == 1) {
			sec = "0000" + (time % (1000 * 60)) + "";
		}
		return min + ":" + sec.trim().substring(0, 2);
	}

	//获取专辑封面的Uri
	private static final Uri albumArtUri = Uri.parse("content://media/external/audio/albumart");
	//获取默认专辑图片
	@SuppressWarnings("ResourceType")
	private static Bitmap getDefaultArtwork(Context context) {
/*		BitmapFactory.Options opts = new BitmapFactory.Options();
		opts.inPreferredConfig = Bitmap.Config.RGB_565;
		return BitmapFactory.decodeStream(context.getResources().openRawResource(R.drawable.ic_cover), null, opts);*/
		Drawable drawable = ContextCompat.getDrawable(context, R.drawable.ic_cover);
		if (drawable instanceof BitmapDrawable) {
			return ((BitmapDrawable) drawable).getBitmap();
		} else if (drawable instanceof VectorDrawable || drawable instanceof VectorDrawableCompat) {
			Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
			Canvas canvas = new Canvas(bitmap);
			drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
			drawable.draw(canvas);
			return bitmap;
		} else {
			throw new IllegalArgumentException("unsupported drawable type");
		}
	}


	//从文件中获取专辑封面位图
	private static Bitmap getArtworkFromFile(Context context, long songid, long albumid){
		Bitmap bm = null;
		if(albumid < 0 && songid < 0) {
			throw new IllegalArgumentException("Must specify an album or a song id");
		}
		try {
			BitmapFactory.Options options = new BitmapFactory.Options();
			FileDescriptor fd = null;
			if(albumid < 0){
				Uri uri = Uri.parse("content://media/external/audio/media/"
						+ songid + "/albumart");
				ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
				if(pfd != null) {
					fd = pfd.getFileDescriptor();
				}
			} else {
				Uri uri = ContentUris.withAppendedId(albumArtUri, albumid);
				ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(uri, "r");
				if(pfd != null) {
					fd = pfd.getFileDescriptor();
				}
			}
			options.inSampleSize = 1;
			// 只进行大小判断
			options.inJustDecodeBounds = true;
			// 得到options得到图片大小
			BitmapFactory.decodeFileDescriptor(fd, null, options);
			// 调用computeSampleSize得到图片缩放比例
			options.inSampleSize = 100;
			// 得到了缩放的比例，读入Bitmap数据
			options.inJustDecodeBounds = false;
			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;

			//根据options参数，减少所需要的内存
			bm = BitmapFactory.decodeFileDescriptor(fd, null, options);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return bm;
	}

	//获取专辑封面位图对象
	public static Bitmap getArtwork(Context context, long song_id, long album_id, boolean allowdefalut){
		if(album_id < 0) {
			if(song_id < 0) {
				Bitmap bm = getArtworkFromFile(context, song_id, -1);
				if(bm != null) {
					return bm;
				}
			}
			if(allowdefalut) {
				return getDefaultArtwork(context);
			}
			return null;
		}
		ContentResolver res = context.getContentResolver();
		Uri uri = ContentUris.withAppendedId(albumArtUri, album_id);
		if(uri != null) {
			InputStream in = null;
			try {
				in = res.openInputStream(uri);
				BitmapFactory.Options options = new BitmapFactory.Options();
				//先制定原始大小
				options.inSampleSize = 1;
				//只进行大小判断
				options.inJustDecodeBounds = true;
				//调用此方法得到options得到图片的大小
				BitmapFactory.decodeStream(in, null, options);
				//调用computeSampleSize得到图片缩放比例, target:800
				options.inSampleSize = computeSampleSize(options, 800);
				//得到缩放比例，读入Bitmap数据
				options.inJustDecodeBounds = false;
				options.inDither = false;
				options.inPreferredConfig = Bitmap.Config.ARGB_8888;
				in = res.openInputStream(uri);
				return BitmapFactory.decodeStream(in, null, options);
			} catch (FileNotFoundException e) {
				Bitmap bm = getArtworkFromFile(context, song_id, album_id);
				if(bm != null) {
					if(bm.getConfig() == null) {
						bm = bm.copy(Bitmap.Config.RGB_565, false);
						if(bm == null && allowdefalut) {
							return getDefaultArtwork(context);
						}
					}
				} else if(allowdefalut) {
					bm = getDefaultArtwork(context);
				}
				return bm;
			} finally {
				try {
					if(in != null) {
						in.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return null;
	}

	//图片缩放
	public static int computeSampleSize(BitmapFactory.Options options, int target) {
		int w = options.outWidth;
		int h = options.outHeight;
		int candidateW = w / target;
		int candidateH = h / target;
		int candidate = Math.max(candidateW, candidateH);
		if(candidate == 0) {
			return 1;
		}
		if(candidate > 1) {
			if((w > target) && (w / candidate) < target) {
				candidate -= 1;
			}
		}
		if(candidate > 1) {
			if((h > target) && (h / candidate) < target) {
				candidate -= 1;
			}
		}
		return candidate;
	}
}
