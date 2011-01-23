package com.appdh.webcamera;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;


import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

public class MainActivity extends Activity 
{
	//�����ļ�
	final String resourceDir = "/sdcard/webcamera/";
	final String tempMP4 = "/sdcard/webcamera/temp.mp4";
	final String configFile = "/sdcard/webcamera/cdata_1.0";

	
	//GUI���ó����Լ�����
	final int bg_screen_bx = 86;
	final int bg_screen_by = 128;
	final int bg_screen_width = 700;
	final int bg_screen_height = 500;
	final int bg_width = 1123;
	final int bg_height = 715;
	final int button_width = 200;
	final int button_height = 80;
	final int live_width = 640;
	final int live_height = 480;
	int screenWidth, screenHeight;
	Button btnSetup, btnStart, btnExit;
	ProgressDialog processMediaDialog;
	
	//��Ҫ���ƶ���
	OverlayView mOverlayView;				//������ʾView
	CameraView	mCameraView;				//Liveͼ��View����
	MediaSource mMediaSource;				//��Ƶ�������
	boolean mSetuped;						//�Ƿ����¼��
	StreamingKernel mStreamingKernel;		//��ȡ��Ƶ���Ķ���
	Thread	  mStreamingKernelThread;		//��ȡ��Ƶ���Ķ����̶߳���
	
	WebServer mWebServer;					//����WebServer����
	Thread	  mWebServerThread;				//����WebServer����,�̶߳���	
	
	//�������ƶ���
	AudioManager mAudioManager = null;		//��ƵЧ��
	
	//Timer����
	Timer detectCameraTimer;
	Timer rePrepareStreamTimer;
	Timer relayMessageTimer;
		
	private void enableButton(Button btn)
	{
		btn.setBackgroundResource(R.drawable.button_active);
		btn.setEnabled(true);
	}
	private void disableButton(Button btn)
	{
		btn.setBackgroundResource(R.drawable.button_inactive);
		btn.setEnabled(false);
	}
	
	private void initLayout()
	{
    	//��ʼ����ʾ
    	requestWindowFeature(Window.FEATURE_NO_TITLE);			//�رձ�����
        Window win = getWindow();
        win.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);           
        win.setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,   
                WindowManager.LayoutParams.FLAG_FULLSCREEN);  	//����ȫ��  

        //�õ���ʾ����ߴ�
    	Display display = ((WindowManager) getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();     	
    	screenWidth = display.getWidth();
    	screenHeight = display.getHeight();
        RelativeLayout.LayoutParams layoutParam = null;		//���ֲ���
    	LayoutInflater myInflate = null;					//����XML������
    	
    	//��ʼ��XML������    	
    	myInflate = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    	
    	//����Top��View
    	RelativeLayout topLayout = new RelativeLayout(this);
    	setContentView(topLayout);
    	LinearLayout preViewLayout = (LinearLayout)myInflate.inflate(R.layout.topview, null);
    	layoutParam = new RelativeLayout.LayoutParams(screenWidth, screenHeight);  
    	topLayout.addView(preViewLayout, layoutParam); 
    	    
    	//���ð�ť
    	int button_width_d = (int)(1.0*screenWidth/bg_width*button_width);
    	int button_height_d = (int)(1.0*screenHeight/bg_height*button_height);

    	btnSetup = new Button(this);
    	btnSetup.setWidth(button_width_d);
    	btnSetup.setHeight(button_height_d);
    	btnSetup.setBackgroundResource(R.drawable.button_active);
    	btnSetup.setText(R.string.setup);
    	layoutParam = new RelativeLayout.LayoutParams(button_width_d,button_height_d);    	
    	layoutParam.topMargin = screenHeight/4;
    	layoutParam.leftMargin = screenWidth - button_width_d*3/2;
    	topLayout.addView(btnSetup, layoutParam);
    	btnSetup.setOnClickListener(mSetupAction);
    	btnSetup.setEnabled(true);
    	
      	btnStart = new Button(this);
    	btnStart.setWidth(button_width_d);
    	btnStart.setHeight(button_height_d);
    	btnStart.setBackgroundResource(R.drawable.button_inactive);
    	btnStart.setText(R.string.start);
    	layoutParam = new RelativeLayout.LayoutParams(button_width_d,button_height_d);    	
    	layoutParam.topMargin = screenHeight/2;
    	layoutParam.leftMargin = screenWidth - button_width_d*3/2;
    	topLayout.addView(btnStart, layoutParam);        	
    	btnStart.setEnabled(false);
    	btnStart.setOnClickListener(mStartAction);
    	
    	btnExit = new Button(this);
    	btnExit.setWidth(button_width_d);
    	btnExit.setHeight(button_height_d);
    	btnExit.setBackgroundResource(R.drawable.button_active);
    	btnExit.setText(R.string.exit);
    	layoutParam = new RelativeLayout.LayoutParams(button_width_d,button_height_d);    	
    	layoutParam.topMargin = screenHeight*3/4;
    	layoutParam.leftMargin = screenWidth - button_width_d*3/2;
    	topLayout.addView(btnExit, layoutParam);    
    	btnExit.setEnabled(true);   
    	btnExit.setOnClickListener(mExitAction);
    	   
    	//����Cameraʵʱͼ����ʾλ��
    	int display_width_d = (int)(1.0*bg_screen_width*screenWidth/bg_width);
    	int display_height_d = (int)(1.0*bg_screen_height*screenHeight/bg_height);    	
    	int prev_rw, prev_rh;
    	if ( 1.0*display_width_d/display_height_d > 1.0*live_width/live_height){    		
    		prev_rh = display_height_d;
    		prev_rw = (int)(1.0*display_height_d * live_width/live_height);
    	} else {
    		prev_rw = display_width_d;
    		prev_rh = (int)(1.0*display_width_d*live_height/live_width);
    	}
    	layoutParam = new RelativeLayout.LayoutParams(prev_rw, prev_rh);
    	layoutParam.topMargin = (int)(1.0*bg_screen_by*screenHeight/bg_height );
    	layoutParam.leftMargin = (int)(1.0*bg_screen_bx*screenWidth/bg_width );
    	
    	//����Ԥ��ͼ��ʵʱ��ʾ
    	mCameraView = new CameraView(this, null);
    	topLayout.addView(mCameraView, layoutParam); 
 
    	//����OverlayView������ʾ    	
    	mOverlayView = new OverlayView(this,null);
    	topLayout.addView(mOverlayView, layoutParam); 
    	if(!mSetuped){
    		mOverlayView.addMessage(R.string.setup_camera);
    		enableButton(btnSetup);
    		disableButton(btnStart);
    		enableButton(btnExit);
    	} else {
    		mOverlayView.addMessage(R.string.setup_ok);
    		disableButton(btnSetup);
    		enableButton(btnStart);
    		enableButton(btnExit);
    	}
	}
	
	private boolean loadSetupedInfo() throws IOException
	{
		File infoFile = new File(configFile);
		InputStream is = new FileInputStream(infoFile.getAbsolutePath());
		byte[] buf = new byte[1024];		
		is.read(buf);		
		MediaDetect.getHeaderData(buf);
		return true;
	}	
	private boolean detectSetuped()
	{
		File infoFile = new File(configFile);
		if ( infoFile.exists()){
			boolean ret;
			try{
				ret = loadSetupedInfo();
			} catch(IOException e) {
				ret = false;
			}			
			return ret;
		}
		return false;
	}
	
	private Handler showRelayMessageHandler = new Handler()
	{
		@Override
        public void handleMessage(Message msg) {        	
			
			mOverlayView.addMessage(msg.what);		
			mOverlayView.postInvalidate();
		}
	};
	
	private Handler endProcessCameraHandler = new Handler()
	{
		@Override
        public void handleMessage(Message msg) {        	
			if( MediaDetect.ppsDataLength > 0 && MediaDetect.spsDataLength > 0){
				MediaDetect.writeConfig(configFile);
				mOverlayView.addMessage(R.string.setup_ok);
				enableButton(btnStart);
			} else {
				mOverlayView.addMessage(R.string.setup_error);
			}
			mOverlayView.postInvalidate();
			processMediaDialog.dismiss();
		}
	};
	
	private Handler beginProcessCameraHandler = new Handler() 
	{
        @Override
        public void handleMessage(Message msg) {        	
			String text = ((Context)MainActivity.this).getString(R.string.setup_process);
			processMediaDialog = ProgressDialog.show(MainActivity.this, "", 
					text, true);
			processMediaDialog.show();
    		
    		Thread processThread = new Thread()
    		{
    	    	@Override
    			public void run(){
    	    		try{
    	    			MediaDetect.checkMP4_MDAT(tempMP4);
    	    			MediaDetect.checkMP4_MOOV(tempMP4);
    	    		} catch (IOException e){
    	    		}    	    		
    	    		endProcessCameraHandler.sendEmptyMessage(0);
    	    	}
    	    };
    	    processThread.start();
         }
	};
	
	private void copyResourceFile(int rid, String targetFile) throws IOException
	{
		InputStream fin = ((Context)this).getResources().openRawResource(rid);
		FileOutputStream fos = new FileOutputStream(targetFile);  
    	
		int     length;
		byte[] buffer = new byte[1024*32]; 
		while( (length = fin.read(buffer)) != -1){
			fos.write(buffer,0,length);
		}
		fin.close();
		fos.close();
	}
	
	private void buildResource()
	{
		String[] str ={"mkdir",resourceDir};

        try { 
        	Process ps = Runtime.getRuntime().exec(str);
        	try {
        		ps.waitFor();
        	} catch (InterruptedException e) {
        		e.printStackTrace();
        	} 
        }
        catch (IOException e) {
        	e.printStackTrace();
        }
        
        //((Context)this).getResources().openRawResource(id)
        //����Դд��SD����ӦĿ¼
        try { 
        	copyResourceFile(R.raw.index, resourceDir + "index.html"  );
        	copyResourceFile(R.raw.player, resourceDir + "player.swf"  );
        	copyResourceFile(R.raw.yt, resourceDir + "yt.swf"  );        	
        }
        catch (IOException e) {
        	e.printStackTrace();
        }
        
	}
	
	private void startDetectCamera()
	{
		mOverlayView.addMessage(R.string.setup_wait);
		mOverlayView.postInvalidate();
		
		buildResource();
		
		mMediaSource.initMedia();
		//mMediaSource.prepareOutput("/dev/null");
		mMediaSource.prepareOutput(tempMP4);		
		mMediaSource.startCapture();
		TimerTask stopDetectCameraTask = new TimerTask()
	    {
	    	@Override
			public void run()
	    	{
	    		//mAudioManager.playSoundEffect(AudioManager.FX_KEYPRESS_SPACEBAR, 1.0f);
	    		mMediaSource.stopCapture();
	    		mMediaSource.releaseMedia();	
	    		beginProcessCameraHandler.sendEmptyMessage(0);
	    	}
	    };
	    
		//Timer����
		if(detectCameraTimer == null ){						
			detectCameraTimer = new Timer();
		}		
		detectCameraTimer.schedule(stopDetectCameraTask, 3000);		
	}
	
	public void relayMessage(final int msg)
	{
		/*
		TimerTask showMessageTask = new TimerTask()
	    {
	    	@Override
			public void run()
	    	{
	    		mOverlayView.addMessage( msg );
				mOverlayView.postInvalidate();
	    	}
	    };
	    
		if(relayMessageTimer == null ){						
			relayMessageTimer = new Timer();
		}
		relayMessageTimer.schedule(showMessageTask, 10);
		*/
		
		showRelayMessageHandler.sendEmptyMessage(msg);
	}
   
    public void doStreaming( OutputStream os)
    {   
    
    	BufferedOutputStream targetOS = new BufferedOutputStream(os, 512); 
    	mMediaSource.startCapture();
    	try {			    	
    		
    		targetOS.write(MediaPackage.FlvHeader);
			MediaPackage.buildVideoHeader(MediaDetect.spsData, MediaDetect.spsDataLength, MediaDetect.ppsData, MediaDetect.ppsDataLength );
			targetOS.write(MediaPackage.videoHeader);
			targetOS.flush();    		
			
		} catch (IOException e1) {                              
			return;
		}   
    	
		byte[] videoBuffer = new byte[1024*64];
		int videoLen;
		
		byte[] tempBuffer;		
		int vflag;
		long ts;
		int tempSize;
		
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) { 
			e.printStackTrace();
		}
		
		
    	while(true){
    		tempBuffer = mStreamingKernel.getReadBuffer();
    		if ( tempBuffer != null){
    			
    			vflag = mStreamingKernel.getVideoFlag();
    			ts = mStreamingKernel.getTimeStamp();
    			tempSize = mStreamingKernel.getReadLength();
    			
    			videoLen = MediaPackage.buildFlvPackage(tempBuffer, tempSize, ts, vflag, videoBuffer);
    			
    			mStreamingKernel.releaseRead();
    			
    			try {
					targetOS.write(videoBuffer, 0, videoLen);
					//targetOS.flush();
				} catch (IOException e) {
					e.printStackTrace();					
					break;
				}
					
    		} else {
    			try {
					Thread.sleep(10);
				} catch (InterruptedException e) { 
					e.printStackTrace();
				}
    		}
    	}
    	
    	try {
			targetOS.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    	
    	stopStreaming();    	
    	rePrepareStreaming();    	
    }
    
    private void rePrepareStreaming()
    {
    	TimerTask rePrepareStreamingTask = new TimerTask()
	    {
	    	@Override
			public void run()
	    	{
	    		prepareStreaming();
	    	}
	    };
	    
    	//Timer����
		if(rePrepareStreamTimer == null ){						
			rePrepareStreamTimer = new Timer();
		}		
		rePrepareStreamTimer.schedule(rePrepareStreamingTask, 1000);	
    }
    
    private void stopStreaming()
    {
    	mStreamingKernel.stopStreaming();
    	mMediaSource.stopCapture();
    }
    
    private void prepareStreaming()
    {
    	mStreamingKernel = new StreamingKernel("appdh.com", 60);			
		mStreamingKernel.repareStreaming();
		
		mMediaSource.initMedia();			
		mMediaSource.prepareOutput(mStreamingKernel.getTargetFileDescriptor());			
		
		mStreamingKernelThread = new Thread( mStreamingKernel );			
		mStreamingKernelThread.start();
    }
    
    
    /** Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);        
        //����Ƿ�̽�������ͷ
        mSetuped = detectSetuped();
        //��ʼ��GUI����
        initLayout();  									        
        //��ʼ��Stream����
        mMediaSource = new MediaSource(mCameraView);	        
        //������Ƶװ��
        if (mAudioManager == null) {					
            mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        }                
    }
    
    private void startWork()
    {    	
		disableButton(btnStart);
		
		//���� Web�������
		mWebServer = new WebServer();		
		WebServer.callBacker = MainActivity.this;
		mWebServerThread = new Thread( mWebServer );
		mWebServerThread.start();
		String url = "http:/" + WebServer.getInterfaces() + ":8080";
		mOverlayView.addMessage( (MainActivity.this).getString(R.string.start_server) + url);
		mOverlayView.postInvalidate();
		
		prepareStreaming();	
		
    }
    
    private OnClickListener mStartAction = new OnClickListener() 
    {
		@Override
		public void onClick(View v) {			
			startWork();
		}
    };

    private OnClickListener mSetupAction = new OnClickListener() 
    {
		@Override
		public void onClick(View v) {
			disableButton(btnSetup);
			startDetectCamera();
		}
    };
    
    private OnClickListener mExitAction = new OnClickListener() 
    {
		@Override
		public void onClick(View v) {
			System.exit(0);
		}
    };
}