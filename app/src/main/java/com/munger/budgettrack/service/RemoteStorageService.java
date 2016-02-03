package com.munger.budgettrack.service;

import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.os.Environment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.DriveContents;
import com.google.android.gms.drive.DriveFile;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResource;
import com.google.android.gms.drive.ExecutionOptions;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.MetadataChangeSet;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.internal.ComparisonFilter;
import com.munger.budgettrack.Main;
import com.munger.budgettrack.model.DBDelta;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;

/**
 * Created by codymunger on 1/18/16.
 */
public class RemoteStorageService implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener
{
    private Main parent;

    protected GoogleApiClient googleClient;

    public RemoteStorageService(Main parent)
    {
        this.parent = parent;
        init();
    }

    public void init()
    {


        googleClient = new GoogleApiClient.Builder(parent)
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    public void connect()
    {
        googleClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle)
    {
        Thread t = new Thread(new Runnable() {public void run()
        {
            Main.instance.dbHelper.syncData();
        }});
        t.start();
    }

    @Override
    public void onConnectionSuspended(int i)
    {

    }


    @Override
    public void onConnectionFailed(ConnectionResult connectionResult)
    {
        if (connectionResult.hasResolution())
        {
            try {
                connectionResult.startResolutionForResult(parent, Main.RESOLVE_CONNECTION_REQUEST_CODE);
            } catch (IntentSender.SendIntentException e) {
                // Unable to resolve, message user appropriately
            }
        } else {
            GooglePlayServicesUtil.getErrorDialog(connectionResult.getErrorCode(), parent, 0).show();
        }
    }

    public ArrayList<DBDelta> getRemoteChangelog(Calendar cal) throws IOException
    {
        DriveFile file = getFileForMonth(cal);
        PendingResult<DriveApi.DriveContentsResult> presult =  file.open(googleClient, DriveFile.MODE_READ_ONLY, null);
        DriveApi.DriveContentsResult result = presult.await();

        if (!result.getStatus().isSuccess())
            throw new IOException("failed to open remote file");

        DriveContents contents = result.getDriveContents();


        ObjectInputStream str = null;
        try
        {
            str = new ObjectInputStream(contents.getInputStream());
        }
        catch(Exception e){
            contents.discard(googleClient);
            return new ArrayList<>();
        }

        ArrayList<DBDelta> ret = new ArrayList<>();

        try
        {
            DBDelta item = (DBDelta) str.readObject();
            ret.add(item);
        }
        catch(ClassNotFoundException e){
            str.close();
            contents.discard(googleClient);
            throw new IOException("failed to parse remote file");
        }
        catch(EOFException e1){
            str.close();
            contents.discard(googleClient);
        }

        return ret;
    }

    public void overwriteRemoteChangeLog(Calendar cal, ArrayList<DBDelta> list) throws IOException
    {
        DriveFile file = getFileForMonth(cal);

        PendingResult<DriveApi.DriveContentsResult> presult2 =  file.open(googleClient, DriveFile.MODE_WRITE_ONLY, null);
        DriveApi.DriveContentsResult result2 = presult2.await();
        DriveContents contents = result2.getDriveContents();
        ObjectOutputStream ostr = new ObjectOutputStream(contents.getOutputStream());

        //for (DBDelta item : list)
        //{
        //    ostr.writeObject(item);
        //}

        ostr.writeChars("Hello world!");

        ostr.flush();

        ExecutionOptions.Builder builder = (new ExecutionOptions.Builder());
        builder.setConflictStrategy(ExecutionOptions.CONFLICT_STRATEGY_KEEP_REMOTE);
        builder.setNotifyOnCompletion(true);
        ExecutionOptions options = builder.build();

        PendingResult<Status> presult3 = contents.commit(googleClient, null, options);
        Status result3 = presult3.await();
        ostr.close();

        if (!result3.isSuccess())
            throw new IOException("remote file was updated after the last read");
    }

    private DriveFile getFileForMonth(Calendar cal)
    {
        String key = getKey(cal);
        DriveFolder folder = Drive.DriveApi.getRootFolder(googleClient);

        Query.Builder builder = new Query.Builder();
        builder.addFilter(Filters.eq(SearchableField.TITLE, key));
        PendingResult<DriveApi.MetadataBufferResult> result3 = folder.queryChildren(googleClient, builder.build());
        DriveApi.MetadataBufferResult bufres = result3.await();
        MetadataBuffer buf = bufres.getMetadataBuffer();
        int sz = buf.getCount();

        DriveFile ret = null;

        if (sz == 0)
        {

            MetadataChangeSet changeSet = new MetadataChangeSet.Builder()
                    .setTitle(key)
                    .setMimeType("application/octet-stream").build();

            PendingResult<DriveApi.DriveContentsResult> result = Drive.DriveApi.newDriveContents(googleClient);
            DriveApi.DriveContentsResult contentRes = result.await();
            DriveContents contents = contentRes.getDriveContents();

            PendingResult<DriveFolder.DriveFileResult> result2 = folder.createFile(googleClient, changeSet, contents);
            DriveFolder.DriveFileResult fileRes = result2.await();

            ret = fileRes.getDriveFile();
        }
        else
        {
            Metadata data = buf.get(0);
            ret = data.getDriveId().asDriveFile();
        }

        buf.release();
        return ret;
    }

    public static String getKey(Calendar cal)
    {
        int month = cal.get(Calendar.MONTH) + 1;
        int year = cal.get(Calendar.YEAR);

        String ret = "" + year;

        if (month < 10)
            ret += "0";

        ret += month;

        return ret;
    }
}
