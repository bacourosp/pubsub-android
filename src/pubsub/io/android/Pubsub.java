package pubsub.io.android;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

import org.json.JSONException;
import org.json.JSONObject;

import android.app.Service;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

/**
 * 
 * @author Andreas G�ransson
 * 
 */
public class Pubsub extends Service {

	private static final String TAG = "Pubsub";
	boolean DEBUG = true;

	public static final float VERSION = 1.0f;

	// Callback constants
	public static final int RAW_TEXT = 10;
	public static final int TERMINATED = 11;
	public static final int ERROR = 12;

	// The binder
	private LocalBinder mBinder = new LocalBinder();

	// The comm
	private PubsubComm mPubsubComm = null;
	private Handler mHandler;

	/**
	 * Class for clients to access. Because we know this service always runs in
	 * the same process as its clients, we don't need to deal with IPC.
	 */
	public class LocalBinder extends Binder {
		public Pubsub getService() {
			return Pubsub.this;
		}
	}

	@Override
	public IBinder onBind(Intent arg0) {
		if (DEBUG)
			Log.i(TAG, "onBind()");

		return mBinder;
	}

	@Override
	public void onCreate() {
		if (DEBUG)
			Log.i(TAG, "onCreate()");

		super.onCreate();
	}

	@Override
	public void onStart(Intent intent, int startId) {
		if (DEBUG)
			Log.i(TAG, "onStart()");

		super.onStart(intent, startId);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		if (DEBUG)
			Log.i(TAG, "onUnbind()");

		return super.onUnbind(intent);
	}

	@Override
	public void onRebind(Intent intent) {
		if (DEBUG)
			Log.i(TAG, "onRebind()");

		super.onRebind(intent);
	}

	@Override
	public void onDestroy() {
		if (DEBUG)
			Log.i(TAG, "onDestroy()");

		super.onDestroy();
	}

	public void connect() {
		// connect("hub.pubsub.io", "10547", "/");
		connect("79.125.4.43", "10547", "/");
	}

	public void connect(String sub) {
		// connect("hub.pubsub.io", "10547", sub);
		connect("79.125.4.43", "10547", sub);
	}

	/**
	 * Connect to hub. We need borth arguments as the same type to easily
	 * forward it to the Task.
	 * 
	 * @param url
	 * @param port
	 */
	public void connect(String host, String port, String sub) {
		if (DEBUG)
			Log.i(TAG, "connect(" + host + "," + port + ")");

		if (mPubsubComm == null) {
			Socket socket = null;
			try {
				socket = new Socket(host, Integer.parseInt(port));
			} catch (NumberFormatException e) {
				Log.e(TAG, "Socket not created", e);
			} catch (UnknownHostException e) {
				Log.e(TAG, "Socket not created", e);
			} catch (IOException e) {
				Log.e(TAG, "Socket not created", e);
			}

			// Connect to pubsub and init thread socket
			if (socket != null) {
				mPubsubComm = new PubsubComm(socket);
				mPubsubComm.execute();
				// Also connect to the selected sub!
				sub(sub);
			} else {
				// If we failed to init the socket, just terminate the app?
				// Might cause
				// null pointers if the handler isn't set though... meh!
				mHandler.obtainMessage(TERMINATED).sendToTarget();
			}
		} else {
			if (DEBUG)
				Log.i(TAG, "Pubsub.io already connected, ignoring");
		}
	}

	/**
	 * Detects if we're subscribed to a sub.
	 * 
	 * @return
	 */
	public boolean isSubscribed() {
		// return (subs.size() > 0);
		return true;
	}

	/**
	 * Hook up to a specific sub.
	 * 
	 * @param sub
	 */
	public void sub(String sub) {
		if (mPubsubComm != null) {
			try {
				write(PubsubParser.sub(sub));
			} catch (JSONException e) {
				if (mHandler != null) {
					JSONObject error = new JSONObject();
					try {
						error.put("simple", "Failed to construct sub message.");
						error.put("error", e.getMessage());
						mHandler.obtainMessage(ERROR, error).sendToTarget();
					} catch (JSONException e1) {
						Log.e(TAG, e.getMessage(), e);
					}
				}
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	/**
	 * Subscribe to a filter on the connected sub.
	 * 
	 * @param json_filter
	 * @param handler_callback
	 */
	public void subscribe(JSONObject json_filter, int handler_callback) {
		if (mPubsubComm != null) {
			try {
				// Send the message to the hub
				write(PubsubParser.subscribe(json_filter, handler_callback));
			} catch (JSONException e) {
				if (mHandler != null) {
					JSONObject error = new JSONObject();
					try {
						error.put("simple",
								"Failed to construct subscribe message.");
						error.put("error", e.getMessage());
						mHandler.obtainMessage(ERROR, error).sendToTarget();
					} catch (JSONException e1) {
						Log.e(TAG, e.getMessage(), e);
					}
				}
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	/**
	 * Unsubscribe from the specified filter.
	 * 
	 * @param handler_callback
	 */
	public void unsubscribe(Integer handler_callback) {
		if (mPubsubComm != null) {
			try {
				// Send the message to the hub
				write(PubsubParser.unsubscribe(handler_callback));
			} catch (JSONException e) {
				if (mHandler != null) {
					JSONObject error = new JSONObject();
					try {
						error.put("simple",
								"Failed to construct unsubscribe message.");
						error.put("error", e.getMessage());
						mHandler.obtainMessage(ERROR, error).sendToTarget();
					} catch (JSONException e1) {
						Log.e(TAG, e.getMessage(), e);
					}
				}
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	public void publish(JSONObject doc) {
		if (mPubsubComm != null) {
			try {
				write(PubsubParser.publish(doc));
			} catch (JSONException e) {
				if (mHandler != null) {
					JSONObject error = new JSONObject();
					try {
						error.put("simple",
								"Failed to construct publish message.");
						error.put("error", e.getMessage());
						mHandler.obtainMessage(ERROR, error).sendToTarget();
					} catch (JSONException e1) {
						Log.e(TAG, e.getMessage(), e);
					}
				}
				Log.e(TAG, e.getMessage(), e);
			}
		}
	}

	/**
	 * 
	 */
	public void disconnect() {
		if (DEBUG)
			Log.i(TAG, "disconnect()");

		if (mPubsubComm != null)
			mPubsubComm.cancel(true);
	}

	/**
	 * Set the callback handler for the service.
	 * 
	 * @param handler
	 */
	public void setHandler(Handler handler) {
		mHandler = handler;
	}

	/**
	 * Basic write, no encoding or formatting.
	 * 
	 * @param message
	 */
	public void write(String message) {
		if (DEBUG)
			Log.i(TAG, "Write: " + message);

		mPubsubComm.write(message.getBytes());
	}

	/**
	 * 
	 * @author Andreas G�ransson
	 * 
	 */
	private class PubsubComm extends AsyncTask<Void, String, Void> {

		private static final String TAG = "PubsubComm";

		private Socket mSocket;
		private InputStream mInputStream;
		private OutputStream mOutputStream;

		public PubsubComm(Socket socket) {
			mSocket = socket;

			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mInputStream = tmpIn;
			mOutputStream = tmpOut;
		}

		@Override
		protected Void doInBackground(Void... params) {
			byte[] buffer = new byte[1024];
			int bytes;

			while (!isCancelled()) {
				try {
					// Read from the InputStream
					bytes = mInputStream.read(buffer);

					if (mHandler != null && bytes > -1) {
						// Always send the raw text
						mHandler.obtainMessage(RAW_TEXT, bytes, -1, buffer)
								.sendToTarget();

						// Parse the message and send to the right callback
						String readMessage = new String(buffer, 0, bytes);
						publishProgress(readMessage);
					}

				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					// TODO, restart the thing if it failed?
					break;
				}

			}

			return null;
		}

		@Override
		protected void onProgressUpdate(String... values) {
			/*
			 * For now we'll use the onProgressUpdate for sending callback
			 * messages back to the activity, this might not be optimal because
			 * the messages might stack and deliver several at once (i.e. they
			 * might not be delivered "on time")
			 */
			for (int i = 0; i < values.length; i++) {
				try {
					JSONObject message = new JSONObject(values[i]);
					int callback_id = message.getInt("id");
					JSONObject doc = message.getJSONObject("doc");
					// Send the message
					mHandler.obtainMessage(callback_id, doc).sendToTarget();
				} catch (JSONException e) {
					Log.e(TAG, e.getMessage(), e);
				}
			}

			super.onProgressUpdate(values);
		}

		@Override
		protected void onCancelled() {
			stop();

			super.onCancelled();
		}

		@Override
		protected void onPostExecute(Void result) {
			stop();

			super.onPostExecute(result);
		}

		/**
		 * Just stops the streams and sends the TERMINATED message to the
		 * activity.
		 */
		private void stop() {
			if (DEBUG)
				Log.i(TAG, "stop()");

			try {
				mInputStream.close();
				mOutputStream.close();
				mSocket.close();
			} catch (IOException e) {
				e.printStackTrace();
			}

			if (mHandler != null)
				mHandler.obtainMessage(TERMINATED).sendToTarget();
		}

		/**
		 * Write a byte buffer to the stream
		 * 
		 * @param buffer
		 */
		public void write(byte[] buffer) {
			try {
				mOutputStream.write(attachHeaderAndFooter(buffer));
			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		/**
		 * This adds the required header and footer for the package, without
		 * them the hub won't recognize the message.
		 * 
		 * @param buffer
		 * @return
		 */
		private byte[] attachHeaderAndFooter(byte[] buffer) {
			// In total, 2 bytes longer than the original message!
			byte[] sendbuffer = new byte[buffer.length + 2];

			// Set the first byte (0x000000)
			sendbuffer[0] = (byte) 0x000000;

			// Add the real package (buffer)
			for (int i = 1; i < sendbuffer.length - 1; i++)
				sendbuffer[i] = buffer[i - 1];

			// Add the footer (0xFFFFFD)
			sendbuffer[sendbuffer.length - 1] = (byte) 0xFFFFFD;

			return sendbuffer;
		}
	}
}
