package br.edu.ufcspa.snorlax_angelo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.TextView;
import android.widget.Toast;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;

import ufcspa.edu.br.sono_angelo_v2.R;




public class RecordFragment extends Fragment {


    private long record_size = 60000; //1 minute
    private static final int RECORDER_BPP = 16;
    private static final String AUDIO_RECORDER_FILE_EXT_WAV = ".wav";
    private static final String AUDIO_RECORDER_FOLDER = "Snore_angELO";
    private static final String AUDIO_RECORDER_TEMP_FILE = "record_temp.raw";
    private static final int RECORDER_SAMPLERATE = 44100;
    static boolean uploadingFile;

    String fileToBeUploaded;
    private static final int RECORDER_CHANNELS = AudioFormat.CHANNEL_IN_MONO;
    private static final int RECORDER_AUDIO_ENCODING = AudioFormat.ENCODING_PCM_16BIT;
    FileInputStream inAudioData = null;
    private static final long BYTES_HORA = 320197200;
    String filename;
    private AudioRecord recorder = null;
    private int bufferSize = 0;
    private Thread recordingThread = null;
    private Thread processingThread = null;
    private boolean isRecording = false;
    private boolean isProcessing = false;
    String fileToprocess = "";
    private int tempNumber=0;


    int serverResponseCode = 0;
    private ProgressDialog dialog = null;

    String upLoadServerUri = "http://angelo.inf.ufrgs.br/snorlax/UploadToServer.php";

    private Chronometer cronometro;
    private Button btn_gravacao;
    private TextView txt_status;
    private TextView txt_cap2;

    private AlertDialog alerta;
    private AlertDialog.Builder builder;













    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    private OnFragmentInteractionListener mListener;

    public RecordFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        return inflater.inflate(R.layout.fragment_record, container, false);

    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        bufferSize = AudioRecord.getMinBufferSize(RECORDER_SAMPLERATE,RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING);

        cronometro = (Chronometer) getView().findViewById(R.id.cronometro);


        // teste do cronometro
        cronometro.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener(){
            @Override
            public void onChronometerTick(Chronometer cArg) {
                long time = SystemClock.elapsedRealtime() - cArg.getBase();
                int h   = (int)(time /3600000);
                int m = (int)(time - h*3600000)/60000;
                int s= (int)(time - h*3600000- m*60000)/1000 ;
                String hh = h < 10 ? "0"+h: h+"";
                String mm = m < 10 ? "0"+m: m+"";
                String ss = s < 10 ? "0"+s: s+"";
                cArg.setText(hh+":"+mm+":"+ss);
            }
        });

        btn_gravacao = ((Button) getView().findViewById(R.id.btn_gravacao));
        btn_gravacao.setOnClickListener(btnClick);

        txt_status = ((TextView)getView().findViewById(R.id.txt_status));

        builder = new AlertDialog.Builder(getView().getContext());
        builder.setTitle("Snore | angELO");
        builder.setMessage("Gravação finalizada e salva na pasta Snore_angELO!");
        builder.setCancelable(false);
        //define um botão como positivo
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface arg0, int arg1) {

            }
        });                                      //:)
        alerta = builder.create();

    }

    // TODO: Rename method, update argument and hook method into UI event
    public void onButtonPressed(Uri uri) {
        if (mListener != null) {
            mListener.onFragmentInteraction(uri);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */
    public interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        void onFragmentInteraction(Uri uri);
    }


    private View.OnClickListener btnClick = new View.OnClickListener() {
        @Override
        public void onClick(View v) {

            if(!isRecording) {
                AppLog.logString("Start Recording");
                startRecording();
                cronometro.setBase(SystemClock.elapsedRealtime());
                cronometro.start();
                btn_gravacao.setText(getString(R.string.btn_parar));
                txt_status.setText(getString(R.string.recording));
                Toast.makeText(getView().getContext(), "Gravação Iniciada!", Toast.LENGTH_SHORT).show();
            }
            else{
                AppLog.logString("Stop Recording");
                stopRecording();
                cronometro.stop();

                alerta.show();

                cronometro.setBase(SystemClock.elapsedRealtime());

                btn_gravacao.setText(getString(R.string.btn_start));
                txt_status.setText(getString(R.string.start_capture));

            }

        }
    };

    private String getFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        return (file.getAbsolutePath() + "/" + "Final_" + System.currentTimeMillis() + AUDIO_RECORDER_FILE_EXT_WAV);
    }

    private String getTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }
        String filename=createFilename();
        File tempFile = new File(filepath,filename+".raw");

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + filename+".raw");
    }

    private String createFilename(){
        tempNumber++;
        Calendar c = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String strDate = sdf.format(c.getTime());
        strDate+="_temp_"+tempNumber;
        return strDate;
    }

    private String listFilesFromDir(){
        String path = Environment.getExternalStorageDirectory().toString()+"/"+AUDIO_RECORDER_FOLDER+"/";
        //Log.d("Files", "Path: " + path);
        File f = new File(path);
        File file[] = f.listFiles();
        try{
            if(file!=null && file.length>0) {
                for (int i=0;i<file.length;i++){
                    Log.d("app", "File in dir: " +file[i].getName() + "  Size: " + file.length);
                }
                return path+file[0].getName();

            }
        }catch (Exception e){
            e.printStackTrace();
        }

        return null;
    }


    private String getFinalTempFilename(){
        String filepath = Environment.getExternalStorageDirectory().getPath();
        File file = new File(filepath,AUDIO_RECORDER_FOLDER);

        if(!file.exists()){
            file.mkdirs();
        }

        File tempFile = new File(filepath,AUDIO_RECORDER_TEMP_FILE);

        if(tempFile.exists())
            tempFile.delete();

        return (file.getAbsolutePath() + "/" + AUDIO_RECORDER_TEMP_FILE);
    }

    private void startRecording(){
        recorder = new AudioRecord(MediaRecorder.AudioSource.MIC,
                RECORDER_SAMPLERATE, RECORDER_CHANNELS,RECORDER_AUDIO_ENCODING, bufferSize);

        recorder.startRecording();

        isRecording = true;

        recordingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                getAudioData();
            }
        },"AudioRecorderActivity Thread");

        recordingThread.start();

        processingThread = new Thread(new Runnable() {

            @Override
            public void run() {
                processAudioData();
            }
        },"Process Audio Data Thread");

        processingThread.start();
    }

    private void getAudioData(){
        fileToprocess = "";
        isProcessing = true;
        while(isRecording) {
            fileToprocess = writeAudioDataToFile(record_size);
        }
        isProcessing = false;
    }

    private void processAudioData(){

/*       Create final processed audio file
        FileOutputStream audioFinal = null;
        filename = getFinalTempFilename();

        try{
            audioFinal = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } */

        while (isProcessing){
            filename = listFilesFromDir();
            try{ //sleeps to try again
                processingThread.sleep(record_size); //wait a minute
            }catch (Exception e) {
                e.printStackTrace();
            }
            if (filename != null && !uploadingFile){ //exist a file to process
                System.out.println("*** PROCESSANDO NOVO TEMP AUDIO");
                try{
                    Log.d("app", "uploading file...");
                    fileToBeUploaded=filename;
                    new UploadFileAsync().execute(fileToBeUploaded);

//

                } catch (Exception e) {
                    e.printStackTrace();
                }

                fileToprocess = "";
            }else if(!uploadingFile){
                Log.d("app", "tried to upload file but another file are being uploaded");
            }else{
                Log.d("app", "there is no files to be uploaded");
            }



        }
    }

    private String writeAudioDataToFile(long record_size){
        byte data[] = new byte[bufferSize];
        String filename = getTempFilename();
        FileOutputStream os = null;
        Long minuteIni;
        Long minuteAtu;
        boolean limiteTime = true;

        try {
            os = new FileOutputStream(filename);
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        int read = 0;

        Log.d("app","*** INICIANDO GRAVAÇÃO");
        if(null != os){
            minuteIni = cronometro.getDrawingTime();
            while(isRecording && limiteTime){
                read = recorder.read(data, 0, bufferSize);

                if(AudioRecord.ERROR_INVALID_OPERATION != read){
                    try {

                        minuteAtu = cronometro.getDrawingTime();
                        if ((minuteAtu - minuteIni) > record_size){
                            Log.d("app","*** UM MINUTO: " + cronometro.getDrawingTime());
                            limiteTime = false;
                        }
                        else{
                            os.write(data);
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }

            try {
                os.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return filename;
    }

    private void stopRecording(){
        if(null != recorder){
            isRecording = false;

            recorder.stop();
            recorder.release();

            recorder = null;
            recordingThread = null;

            processingThread = null;
        }

    }

    private void deleteFinalTempFile() {
        File file = new File(getFinalTempFilename());
        file.delete();
    }

    private void deleteTempFile(String filename) {
        File file = new File(filename);
        file.delete();
    }

    private void copyWaveFile(String inFilename,String outFilename){
        FileInputStream in = null;
        FileOutputStream out = null;
        long totalAudioLen = 0;
        long totalDataLen = totalAudioLen + 36;
        long longSampleRate = RECORDER_SAMPLERATE;
        int channels = 1; //2
        long byteRate = RECORDER_BPP * RECORDER_SAMPLERATE * channels/8;

        byte[] data = new byte[bufferSize];

        try {
            in = new FileInputStream(inFilename);
            out = new FileOutputStream(outFilename);
            totalAudioLen = in.getChannel().size();
            totalDataLen = totalAudioLen + 36;

            AppLog.logString("File size: " + totalDataLen);

            WriteWaveFileHeader(out, totalAudioLen, totalDataLen,
                    longSampleRate, channels, byteRate);

            while(in.read(data) != -1){
                out.write(data);
            }

            in.close();
            out.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void WriteWaveFileHeader(
            FileOutputStream out, long totalAudioLen,
            long totalDataLen, long longSampleRate, int channels,
            long byteRate) throws IOException {

        byte[] header = new byte[44];

        header[0] = 'R';  // RIFF/WAVE header
        header[1] = 'I';
        header[2] = 'F';
        header[3] = 'F';
        header[4] = (byte) (totalDataLen & 0xff);
        header[5] = (byte) ((totalDataLen >> 8) & 0xff);
        header[6] = (byte) ((totalDataLen >> 16) & 0xff);
        header[7] = (byte) ((totalDataLen >> 24) & 0xff);
        header[8] = 'W';
        header[9] = 'A';
        header[10] = 'V';
        header[11] = 'E';
        header[12] = 'f';  // 'fmt ' chunk
        header[13] = 'm';
        header[14] = 't';
        header[15] = ' ';
        header[16] = 16;  // 4 bytes: size of 'fmt ' chunk
        header[17] = 0;
        header[18] = 0;
        header[19] = 0;
        header[20] = 1;  // format = 1
        header[21] = 0;
        header[22] = (byte) channels;
        header[23] = 0;
        header[24] = (byte) (longSampleRate & 0xff);
        header[25] = (byte) ((longSampleRate >> 8) & 0xff);
        header[26] = (byte) ((longSampleRate >> 16) & 0xff);
        header[27] = (byte) ((longSampleRate >> 24) & 0xff);
        header[28] = (byte) (byteRate & 0xff);
        header[29] = (byte) ((byteRate >> 8) & 0xff);
        header[30] = (byte) ((byteRate >> 16) & 0xff);
        header[31] = (byte) ((byteRate >> 24) & 0xff);
        header[32] = (byte) (2 * 16 / 8);  // block align
        header[33] = 0;
        header[34] = RECORDER_BPP;  // bits per sample
        header[35] = 0;
        header[36] = 'd';
        header[37] = 'a';
        header[38] = 't';
        header[39] = 'a';
        header[40] = (byte) (totalAudioLen & 0xff);
        header[41] = (byte) ((totalAudioLen >> 8) & 0xff);
        header[42] = (byte) ((totalAudioLen >> 16) & 0xff);
        header[43] = (byte) ((totalAudioLen >> 24) & 0xff);

        out.write(header, 0, 44);
    }



}
