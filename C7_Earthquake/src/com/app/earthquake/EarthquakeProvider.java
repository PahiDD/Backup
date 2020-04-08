package com.app.earthquake;

import android.content.*;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDatabase.CursorFactory;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class EarthquakeProvider extends ContentProvider
	{
		public static final Uri CONTENT_URI = Uri.parse("content://com.app.provider.earthquake/earthquakes");

		@Override
		public boolean onCreate()
			{
				Context context = getContext();
				earthquakeDatabaseHelper dbHelper = new earthquakeDatabaseHelper(context, DATABASE_NAME, null,
						DATABASE_VERSION);
				earthquakeDB = dbHelper.getWritableDatabase();
				return (earthquakeDB == null) ? false : true;
			}

		@Override
		public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sort)
			{
				SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
				qb.setTables(EARTHQUAKE_TABLE);
				// Если это запрос к одной строке, ограничьте результат требуемой строкой
				switch (uriMatcher.match(uri))
					{
					case QUAKE_ID:
						qb.appendWhere(KEY_ID + "=" + uri.getPathSegments().get(1));
						break;
					default:
						break;
					}
				// Если не указан порядок сортировки, сортируйте по дате/времени
				String orderBy;
				if (TextUtils.isEmpty(sort))
					{
						orderBy = KEY_DATE;
					} else
					{
						orderBy = sort;
					}
				// Примените запрос к исходной базе данных.
				Cursor c = qb.query(earthquakeDB, projection, selection, selectionArgs, null, null, orderBy);
				// Зарегистрируйте ContentResolver, принадлежащий объекту Context, чтобы
				// получать уведомления об изменении результирующего набора данных.
				c.setNotificationUri(getContext().getContentResolver(), uri);
				// Верните курсор, ссылающийся на результат запроса.
				return c;
			}

		@Override
		public Uri insert(Uri _uri, ContentValues _initialValues)
			{
				// Вставьте новую строку, в случае успеха будет возвращен
				// номер строки.
				long rowID = earthquakeDB.insert(EARTHQUAKE_TABLE, "quake", _initialValues);
				// Верните путь URI к только что вставленной строке
				// (в случае успеха).
				if (rowID > 0)
					{
						Uri uri = ContentUris.withAppendedId(CONTENT_URI, rowID);
						getContext().getContentResolver().notifyChange(uri, null);
						return uri;
					}
				throw new SQLException("Failed to insert row into " + _uri);
			}

		@Override
		public int delete(Uri uri, String where, String[] whereArgs)
			{
				int count;
				switch (uriMatcher.match(uri))
					{
					case QUAKES:
						count = earthquakeDB.delete(EARTHQUAKE_TABLE, where, whereArgs);
						break;
					case QUAKE_ID:
						String segment = uri.getPathSegments().get(1);
						count = earthquakeDB.delete(EARTHQUAKE_TABLE,
								KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
								whereArgs);
						break;
					default:
						throw new IllegalArgumentException("delete-Unsupported URI: " + uri);
					}
				getContext().getContentResolver().notifyChange(uri, null);
				return count;
			}

		@Override
		public int update(Uri uri, ContentValues values, String where, String[] whereArgs)
			{
				int count;
				switch (uriMatcher.match(uri))
					{
					case QUAKES:
						count = earthquakeDB.update(EARTHQUAKE_TABLE, values, where, whereArgs);
						break;
					case QUAKE_ID:
						String segment = uri.getPathSegments().get(1);
						count = earthquakeDB.update(EARTHQUAKE_TABLE, values,
								KEY_ID + "=" + segment + (!TextUtils.isEmpty(where) ? " AND (" + where + ')' : ""),
								whereArgs);
						break;
					default:
						throw new IllegalArgumentException("Unknown URI " + uri);
					}
				getContext().getContentResolver().notifyChange(uri, null);
				return count;
			}

		@Override
		public String getType(Uri uri)
			{
				switch (uriMatcher.match(uri))
					{
					case QUAKES:
						return "vnd.android.cursor.dir/vnd.app.earthquake";
					case QUAKE_ID:
						return "vnd.android.cursor.item/vnd.app.earthquake";
					default:
						throw new IllegalArgumentException("Unsupported URI: " + uri);
					}
			}

		// Создайте константы для запросов к разным URI.
		private static final int QUAKES = 1;
		private static final int QUAKE_ID = 2;
		private static final UriMatcher uriMatcher;
		// Создайте объект UriMatcher, сделайте так, чтобы путь URI, заканчивающийся на
		// ‘earthquakes’, соответствовал запросу ко всем землетрясениям, а URI,
		// имеющий окончание ‘/[rowID]’, соответствовал одиночной строке с
		// информацией о землетрясении.
		static
			{
				uriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
				uriMatcher.addURI("com.app.provider.Earthquake", "earthquakes", QUAKES);
				uriMatcher.addURI("com.app.provider.Earthquake", "earthquakes/#", QUAKE_ID);
			}

//Исходная база данных
		private SQLiteDatabase earthquakeDB;
		private static final String TAG = "EarthquakeProvider";
		private static final String DATABASE_NAME = "earthquakes.db";
		private static final int DATABASE_VERSION = 1;
		private static final String EARTHQUAKE_TABLE = "earthquakes";
//Имена столбцов
		public static final String KEY_ID = "_id";
		public static final String KEY_DATE = "date";
		public static final String KEY_DETAILS = "details";
		public static final String KEY_LOCATION_LAT = "latitude";
		public static final String KEY_LOCATION_LNG = "longitude";
		public static final String KEY_MAGNITUDE = "magnitude";
		public static final String KEY_LINK = "link";
//Индексы столбцов
		public static final int DATE_COLUMN = 1;
		public static final int DETAILS_COLUMN = 2;
		public static final int LATITUDE_COLUMN = 3;
		public static final int LONGITUDE_COLUMN = 4;
		public static final int MAGNITUDE_COLUMN = 5;
		public static final int LINK_COLUMN = 6;

//Вспомогательный класс для открытия, создания и управления контролем версий базы данных
		private static class earthquakeDatabaseHelper extends SQLiteOpenHelper
			{
				private static final String DATABASE_CREATE = "create table " + EARTHQUAKE_TABLE + " (" + KEY_ID
						+ " integer primary key autoincrement, " + KEY_DATE + " INTEGER, " + KEY_DETAILS + " TEXT, "
						+ KEY_LOCATION_LAT + " FLOAT, " + KEY_LOCATION_LNG + " FLOAT, " + KEY_MAGNITUDE + " FLOAT, "
						+ KEY_LINK + " TEXT);";

				public earthquakeDatabaseHelper(Context context, String name, CursorFactory factory, int version)
					{
						super(context, name, factory, version);
					}

				@Override
				public void onCreate(SQLiteDatabase db)
					{
						db.execSQL(DATABASE_CREATE);
					}

				@Override
				public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion)
					{
						Log.w(TAG, "Upgrading database from version " + oldVersion + " to " + newVersion
								+ ", which will destroy all old data");
						db.execSQL("DROP TABLE IF EXISTS " + EARTHQUAKE_TABLE);
						onCreate(db);
					}
			}
	}
