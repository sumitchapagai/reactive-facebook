/**
 * Copyright (c) 2015-present, Facebook, Inc. All rights reserved.
 *
 * <p>This source code is licensed under the BSD-style license found in the LICENSE file in the root
 * directory of this source tree. An additional grant of patent rights can be found in the PATENTS
 * file in the same directory.
 */
package com.facebook.react.modules.blob;

import android.content.ContentResolver;
import android.content.Context;
import android.content.res.Resources;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.webkit.MimeTypeMap;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableArray;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.common.MapBuilder;
import com.facebook.react.module.annotations.ReactModule;
import com.facebook.react.modules.network.NetworkingModule;
import com.facebook.react.modules.websocket.WebSocketModule;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import okio.ByteString;

@ReactModule(name = BlobModule.NAME)
public class BlobModule extends ReactContextBaseJavaModule {

  protected static final String NAME = "BlobModule";

  private static final Map<String, byte[]> mBlobs = new HashMap<>();

  protected static final WebSocketModule.ContentHandler BlobHandler =
      new WebSocketModule.ContentHandler() {
        @Override
        public void onMessage(String text, WritableMap params) {
          params.putString("data", text);
        }

        @Override
        public void onMessage(ByteString bytes, WritableMap params) {
          byte[] data = bytes.toByteArray();

          WritableMap blob = Arguments.createMap();

          blob.putString("blobId", store(data));
          blob.putInt("offset", 0);
          blob.putInt("size", data.length);

          params.putMap("data", blob);
          params.putString("type", "blob");
        }
      };

  private static final NetworkingModule.UriHandler NetworkingUriHandler =
          new NetworkingModule.UriHandler() {
            @Override
            public boolean supports(Uri uri, String responseType) {
              String scheme = uri.getScheme();
              boolean isRemote = scheme.equals("http") || scheme.equals("https");

              return  (!isRemote && responseType.equals("blob"));
            }

            @Override
            public WritableMap fetch(Uri uri, Context context) throws IOException {
              ContentResolver resolver = context.getContentResolver();

              byte[] data = getBytesFromUri(uri, resolver);

              WritableMap blob = Arguments.createMap();
              blob.putString("blobId", store(data));
              blob.putInt("offset", 0);
              blob.putInt("size", data.length);
              blob.putString("type", getMimeTypeFromUri(uri, resolver));

              // Needed for files
              blob.putString("name", getNameFromUri(uri, resolver));
              blob.putDouble("lastModified", getLastModifiedFromUri(uri));

              return blob;
            }
          };

  private static final NetworkingModule.RequestBodyHandler NetworkingRequestBodyHandler =
          new NetworkingModule.RequestBodyHandler() {
            @Override
            public boolean supports(ReadableMap data) {
              return data.hasKey("blob");
            }

            @Override
            public RequestBody toRequestBody(ReadableMap data, String contentType) {
              String type = contentType;
              if (data.hasKey("type") && !data.getString("type").isEmpty()) {
                type = data.getString("type");
              }
              if (type == null) {
                type = "application/octet-stream";
              }
              ReadableMap blob = data.getMap("blob");
              String blobId = blob.getString("blobId");
              byte[] bytes = resolve(
                      blobId,
                      blob.getInt("offset"),
                      blob.getInt("size"));;

              return RequestBody.create(MediaType.parse(type), bytes);
            }
          };

  private static final NetworkingModule.ResponseHandler NetworkingResponseHandler =
          new NetworkingModule.ResponseHandler() {
            @Override
            public boolean supports(String responseType) {
              return responseType.equals("blob");
            }

            @Override
            public WritableMap toResponseData(ResponseBody body) throws IOException {
              byte[] data = body.bytes();
              WritableMap blob = Arguments.createMap();
              blob.putString("blobId", store(data));
              blob.putInt("offset", 0);
              blob.putInt("size", data.length);
              return blob;
            }
          };

  public BlobModule(ReactApplicationContext reactContext) {
    super(reactContext);
  }

  @Override
  public String getName() {
    return NAME;
  }

  @Override
  @Nullable
  public Map getConstants() {
    // The application can register BlobProvider as a ContentProvider so that blobs are resolvable.
    // If it does, it needs to tell us what authority was used via this string resource.
    Resources resources = getReactApplicationContext().getResources();
    String packageName = getReactApplicationContext().getPackageName();
    int resourceId = resources.getIdentifier("blob_provider_authority", "string", packageName);
    if (resourceId == 0) {
      return null;
    }

    return MapBuilder.of(
        "BLOB_URI_SCHEME", "content", "BLOB_URI_HOST", resources.getString(resourceId));
  }

  public static String store(byte[] data) {
    String blobId = UUID.randomUUID().toString();
    store(data, blobId);
    return blobId;
  }

  public static void store(byte[] data, String blobId) {
    mBlobs.put(blobId, data);
  }

  public static void remove(String blobId) {
    mBlobs.remove(blobId);
  }

  @Nullable
  public static byte[] resolve(Uri uri) {
    String blobId = uri.getLastPathSegment();
    int offset = 0;
    int size = -1;
    String offsetParam = uri.getQueryParameter("offset");
    if (offsetParam != null) {
      offset = Integer.parseInt(offsetParam, 10);
    }
    String sizeParam = uri.getQueryParameter("size");
    if (sizeParam != null) {
      size = Integer.parseInt(sizeParam, 10);
    }
    return resolve(blobId, offset, size);
  }

  @Nullable
  public static byte[] resolve(String blobId, int offset, int size) {
    byte[] data = mBlobs.get(blobId);
    if (data == null) {
      return null;
    }
    if (size == -1) {
      size = data.length - offset;
    }
    if (offset >= 0) {
      data = Arrays.copyOfRange(data, offset, offset + size);
    }
    return data;
  }

  @Nullable
  public static byte[] resolve(ReadableMap blob) {
    return resolve(blob.getString("blobId"), blob.getInt("offset"), blob.getInt("size"));
  }

  private static byte[] getBytesFromUri(Uri contentUri, ContentResolver resolver) throws IOException {
    InputStream is = resolver.openInputStream(contentUri);

    if (is == null) {
      throw new FileNotFoundException("File not found for " + contentUri);
    }

    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
    int bufferSize = 1024;
    byte[] buffer = new byte[bufferSize];
    int len;
    while ((len = is.read(buffer)) != -1) {
      byteBuffer.write(buffer, 0, len);
    }
    return byteBuffer.toByteArray();
  }

  private static String getNameFromUri(Uri contentUri, ContentResolver resolver) {
    if (contentUri.getScheme().equals("file")) {
      return contentUri.getLastPathSegment();
    }
    String[] projection = {MediaStore.MediaColumns.DISPLAY_NAME};
    Cursor metaCursor = resolver.query(contentUri, projection, null, null, null);
    if (metaCursor != null) {
      try {
        if (metaCursor.moveToFirst()) {
          return metaCursor.getString(0);
        }
      } finally {
        metaCursor.close();
      }
    }
    return contentUri.getLastPathSegment();
  }

  private static long getLastModifiedFromUri(Uri contentUri) {
    if (contentUri.getScheme().equals("file")) {
      return new File(contentUri.toString()).lastModified();
    }
    return 0;
  }

  private static String getMimeTypeFromUri(Uri contentUri, ContentResolver resolver) {
    String type = resolver.getType(contentUri);

    if (type == null) {
      String ext = MimeTypeMap.getFileExtensionFromUrl(contentUri.getPath());
      if (ext != null) {
        type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext);
      }
    }

    if (type == null) {
      type = "";
    }

    return type;
  }

  @ReactMethod
  public void addXMLHttpRequestHandler() {
    NetworkingModule.addUriHandler(NetworkingUriHandler);
    NetworkingModule.addRequestBodyHandler(NetworkingRequestBodyHandler);
    NetworkingModule.addResponseHandler(NetworkingResponseHandler);
  }

  @ReactMethod
  public void removeXMLHttpRequestHandler() {
    NetworkingModule.removeUriHandler(NetworkingUriHandler);
    NetworkingModule.removeRequestBodyHandler(NetworkingRequestBodyHandler);
    NetworkingModule.removeResponseHandler(NetworkingResponseHandler);
  }

  private WebSocketModule getWebSocketModule() {
    return getReactApplicationContext().getNativeModule(WebSocketModule.class);
  }

  @ReactMethod
  public void addWebSocketHandler(final int id) {
    getWebSocketModule().setContentHandler(id, BlobHandler);
  }

  @ReactMethod
  public void removeWebSocketHandler(final int id) {
    getWebSocketModule().setContentHandler(id, null);
  }

  @ReactMethod
  public void sendOverSocket(ReadableMap blob, int id) {
    byte[] data = resolve(blob.getString("blobId"), blob.getInt("offset"), blob.getInt("size"));

    if (data != null) {
      getWebSocketModule().sendBinary(ByteString.of(data), id);
    } else {
      getWebSocketModule().sendBinary((ByteString) null, id);
    }
  }

  @ReactMethod
  public void createFromParts(ReadableArray parts, String blobId) {
    int totalBlobSize = 0;
    ArrayList<byte[]> partList = new ArrayList<>(parts.size());
    for (int i = 0; i < parts.size(); i++) {
      ReadableMap part = parts.getMap(i);
      switch (part.getString("type")) {
        case "blob":
          ReadableMap blob = part.getMap("data");
          totalBlobSize += blob.getInt("size");
          partList.add(i, resolve(blob));
          break;
        case "string":
          byte[] bytes = part.getString("data").getBytes(Charset.forName("UTF-8"));
          totalBlobSize += bytes.length;
          partList.add(i, bytes);
          break;
        default:
          throw new IllegalArgumentException("Invalid type for blob: " + part.getString("type"));
      }
    }
    ByteBuffer buffer = ByteBuffer.allocate(totalBlobSize);
    for (byte[] bytes : partList) {
      buffer.put(bytes);
    }
    store(buffer.array(), blobId);
  }

  @ReactMethod
  public void release(String blobId) {
    remove(blobId);
  }
}
