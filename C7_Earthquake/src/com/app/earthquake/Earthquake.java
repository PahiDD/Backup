package com.app.earthquake;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView.OnItemClickListener;

public class Earthquake extends Activity
	{
		static final private int MENU_UPDATE = Menu.FIRST;
		static final private int MENU_PREFERENCES = Menu.FIRST + 1;
		static final private int QUAKE_DIALOG = 1;
		static final private int DATE_DIALOG = 2;
		private static final int SHOW_PREFERENCES = 1;

		int minimumMagnitude = 0;
		int maximumMagnitude = 0;
		boolean autoUpdate = false;
		int updateFreq = 0;

		TextView TextVievMagnitude, TextVievProviderCount, TextVievCurrentDate, TextVievDateSelectedItem,
				TextVievDateBoolean;
		ListView ListViewEarthquake;
		ArrayAdapter<Quake> aa;
		ArrayList<Quake> earthquakes = new ArrayList<Quake>();
		SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");

		Quake selectedQuake;

		@Override
		public void onCreate(Bundle icicle)
			{
				super.onCreate(icicle);
				setContentView(R.layout.earthquake_main);

				TextVievMagnitude = (TextView) this.findViewById(R.id.TextVievMagnitude);
				TextVievProviderCount = (TextView) this.findViewById(R.id.TextVievProviderCount);
				TextVievCurrentDate = (TextView) this.findViewById(R.id.TextVievCurrentDate);
				TextVievDateSelectedItem = (TextView) this.findViewById(R.id.TextVievDateSelectedItem);
				TextVievDateBoolean = (TextView) this.findViewById(R.id.TextVievDateBoolean);

				ListViewEarthquake = (ListView) this.findViewById(R.id.ListViewEarthquake);

				Date date = new Date();
				String dt = sdf.format(date.getTime());

				TextVievCurrentDate.setText(getString(R.string.TextVievCurrentDate) + ": " + dt);
				TextVievDateSelectedItem.setText(getString(R.string.TextVievDateSelectedItem) + ": ");
				TextVievDateBoolean.setText(getString(R.string.TextVievDateBoolean) + ": ");

				ContentResolver cr = getContentResolver();
				// Uri uri = Uri.parse("content://com.app.provider.earthquake/earthquakes/2");
				// cr.delete(uri, "_id < 5", null);

				ListViewEarthquake.setOnItemClickListener(new OnItemClickListener()
					{

						@Override
						public void onItemClick(AdapterView _av, View _v, int _index, long arg3)
							{
								selectedQuake = earthquakes.get(_index);
								showDialog(QUAKE_DIALOG);
							}
					});

				int layoutID = android.R.layout.simple_list_item_1;
				aa = new ArrayAdapter<Quake>(this, layoutID, earthquakes);
				ListViewEarthquake.setAdapter(aa);

				// DeleteItemsFromProvider();

				// loadQuakesFromProvider();
				updateFromPreferences();
				refreshEarthquakes();
			}

		private void refreshEarthquakes()
			{
				// Получите XML
				URL url;
				try
					{
						String quakeFeed = getString(R.string.quake_feed);
						url = new URL(quakeFeed);

						URLConnection connection;
						connection = url.openConnection();

						HttpURLConnection httpConnection = (HttpURLConnection) connection;
						int responseCode = httpConnection.getResponseCode();

						if (responseCode == HttpURLConnection.HTTP_OK)
							{
								InputStream in = httpConnection.getInputStream();

								DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
								DocumentBuilder db = dbf.newDocumentBuilder();

								// Разберите поток землетрясений.
								Document dom = db.parse(in);
								Element docEle = dom.getDocumentElement();

								// Очистите старые землетрясения
								earthquakes.clear();
								//loadQuakesFromProvider();

								// Получите список всех землетрясений.
								NodeList nl = docEle.getElementsByTagName("entry");
								if (nl != null && nl.getLength() > 0)
									{
										for (int i = 0; i < nl.getLength(); i++)
											{
												Element entry = (Element) nl.item(i);
												Element title = (Element) entry.getElementsByTagName("title").item(0);
												Element g = (Element) entry.getElementsByTagName("georss:point")
														.item(0);
												Element when = (Element) entry.getElementsByTagName("updated").item(0);
												Element link = (Element) entry.getElementsByTagName("link").item(0);

												String details = title.getFirstChild().getNodeValue();
												// String hostname = "http://earthquake.usgs.gov";
												String linkString = link.getAttribute("href");
												String point = g.getFirstChild().getNodeValue();
												String dt = when.getFirstChild().getNodeValue();
												SimpleDateFormat sdfxml = new SimpleDateFormat(
														"yyyy-MM-dd'T'hh:mm:ss.ms'Z'"); // 2020-03-29T08:50:56.117Z
												Date qdate = new GregorianCalendar(0, 0, 0).getTime();
												try
													{
														qdate = sdfxml.parse(dt);
													} catch (ParseException e)
													{
														e.printStackTrace();
													}

												String[] location = point.split(" ");
												Location l = new Location("dummyGPS");
												l.setLatitude(Double.parseDouble(location[0]));
												l.setLongitude(Double.parseDouble(location[1]));

												String magnitudeString = details.split(" ")[1];
												magnitudeString.length();
												double magnitude = Double.parseDouble(magnitudeString);
												// details = details.split(" - ")[1].trim();
												Quake quake = new Quake(qdate, details, l, magnitude, linkString);
												// Обработайте только что найденное землетрясение
												// addNewQuake(quake);
											}
									}
							}
					} catch (MalformedURLException e)
					{
						e.printStackTrace();
					} catch (IOException e)
					{
						e.printStackTrace();
					} catch (ParserConfigurationException e)
					{
						e.printStackTrace();
					} catch (SAXException e)
					{
						e.printStackTrace();
					} finally
					{
					}
			}

		private void addNewQuake(Quake _quake)
			{
				ContentResolver cr = getContentResolver();
				// Создайте оператор WHERE, чтобы быть уверенным, что Источник данных
				// уже не содержит это землетрясение.
				String w = EarthquakeProvider.KEY_DATE + " = " + _quake.getDate().getTime();
				// Если землетрясение новое, вставьте его в Источник данных.
				if (cr.query(EarthquakeProvider.CONTENT_URI, null, w, null, null).getCount() == 0)
					{
						ContentValues values = new ContentValues();
						values.put(EarthquakeProvider.KEY_DATE, _quake.getDate().getTime());
						values.put(EarthquakeProvider.KEY_DETAILS, _quake.getDetails());

						double lat = _quake.getLocation().getLatitude();
						double lng = _quake.getLocation().getLongitude();
						values.put(EarthquakeProvider.KEY_LOCATION_LAT, lat);
						values.put(EarthquakeProvider.KEY_LOCATION_LNG, lng);
						values.put(EarthquakeProvider.KEY_LINK, _quake.getLink());
						values.put(EarthquakeProvider.KEY_MAGNITUDE, _quake.getMagnitude());

						cr.insert(EarthquakeProvider.CONTENT_URI, values);
						earthquakes.add(_quake);
						addQuakeToArray(_quake);
					}
			}

		private void addQuakeToArray(Quake _quake)
			{
				if (_quake.getMagnitude() >= minimumMagnitude && _quake.getMagnitude() < maximumMagnitude)
					{
						// Добавьте новое землетрясение в наш список.
						earthquakes.add(_quake);
						// Оповестите Адаптер массива об изменениях.
						aa.notifyDataSetChanged();
					}
			}

		private void DeleteItemsFromProvider()
			{
				ContentResolver cr = getContentResolver();
				String where = "_id < 5";
				cr.delete(EarthquakeProvider.CONTENT_URI, where, null);
			}

		private void loadQuakesFromProvider()
			{
				// Очистите существующий массив с землетрясениями
				earthquakes.clear();
				ContentResolver cr = getContentResolver();
				// Верните все сохраненные землетрясения
				Cursor c = cr.query(EarthquakeProvider.CONTENT_URI, null, null, null, null);
				if (c.moveToFirst())
					{
						do
							{
								// Извлеките информацию о землетрясении.
								Long datems = c.getLong(EarthquakeProvider.DATE_COLUMN);
								String details = c.getString(EarthquakeProvider.DETAILS_COLUMN);
								Float lat = c.getFloat(EarthquakeProvider.LATITUDE_COLUMN);
								Float lng = c.getFloat(EarthquakeProvider.LONGITUDE_COLUMN);
								Double mag = c.getDouble(EarthquakeProvider.MAGNITUDE_COLUMN);
								String link = c.getString(EarthquakeProvider.LINK_COLUMN);
								Location location = new Location("dummy");
								location.setLongitude(lng);
								location.setLatitude(lat);
								Date date = new Date(datems);
								Quake q = new Quake(date, details, location, mag, link);
								addQuakeToArray(q);
							} while (c.moveToNext());
					}
				TextVievProviderCount.setText(getString(R.string.TextVievProviderCount) + ": " + c.getCount());
			}

		@Override
		public boolean onCreateOptionsMenu(Menu menu)
			{
				super.onCreateOptionsMenu(menu);
				menu.add(0, MENU_UPDATE, Menu.NONE, R.string.menu_update);
				menu.add(0, MENU_PREFERENCES, Menu.NONE, R.string.menu_preferences);
				return true;
			}

		public boolean onOptionsItemSelected(MenuItem item)
			{
				super.onOptionsItemSelected(item);
				switch (item.getItemId())
					{
					case (MENU_UPDATE):
						{
							updateFromPreferences();
							refreshEarthquakes();
							return true;
						}
					case (MENU_PREFERENCES):
						{
							Intent i = new Intent(this, Preferences.class);
							startActivityForResult(i, SHOW_PREFERENCES);
							return true;
						}
					}
				return false;
			}

		@Override
		public Dialog onCreateDialog(int id)
			{
				switch (id)
					{
					case (QUAKE_DIALOG):
					case (DATE_DIALOG):
						LayoutInflater li = LayoutInflater.from(this);
						View quakeDetailsView = li.inflate(R.layout.quake_details, null);
						AlertDialog.Builder quakeDialog = new AlertDialog.Builder(this);
						quakeDialog.setTitle(R.string.prefs_quake_mag_title);
						quakeDialog.setView(quakeDetailsView);
						return quakeDialog.create();
					}
				return null;
			}

		@Override
		public void onPrepareDialog(int id, Dialog dialog)
			{
				ContentResolver cr = getContentResolver();
				Quake _quake = null;
				String dateString, quakeText;
				AlertDialog alertDialog = (AlertDialog) dialog;
				TextView tv = (TextView) alertDialog.findViewById(R.id.quakeDetailsTextView);

				String w = EarthquakeProvider.KEY_DATE + " < " + _quake.getDate().getTime();

				switch (id)
					{
					case (QUAKE_DIALOG):
						dateString = sdf.format(selectedQuake.getDate());
						quakeText = getString(R.string.prefs_quake_mag_title) + ": " + selectedQuake.getMagnitude()
								+ "\n" + selectedQuake.getDetails() + "\n" + selectedQuake.getLink();
						alertDialog.setTitle(dateString);
						tv.setText(quakeText);
						break;

					case (DATE_DIALOG):
						dateString = sdf.format(selectedQuake.getDate());
						quakeText = "";
						alertDialog.setTitle(dateString);
						tv.setText(quakeText);
						break;
					}
			}

		private void updateFromPreferences()
			{
				Context context = getApplicationContext();
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

				minimumMagnitude = Integer.parseInt(prefs.getString(Preferences.PREF_MIN_MAG, "0"));
				updateFreq = Integer.parseInt(prefs.getString(Preferences.PREF_UPDATE_FREQ, "0"));
				autoUpdate = prefs.getBoolean(Preferences.PREF_AUTO_UPDATE, false);

				maximumMagnitude = minimumMagnitude + 1;
				TextVievMagnitude.setText(getString(R.string.prefs_quake_mag_title) + " от " + minimumMagnitude + " до "
						+ maximumMagnitude);
			}

		@Override
		public void onActivityResult(int requestCode, int resultCode, Intent data)
			{
				super.onActivityResult(requestCode, resultCode, data);
				if (requestCode == SHOW_PREFERENCES)
					if (resultCode == Activity.RESULT_CANCELED)
						{
							updateFromPreferences();
							refreshEarthquakes();
						}
			}

	}
