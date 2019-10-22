package com.example.handlerdemo;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.MessageQueue;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    TextView textView;
    Button button;


    //这个Handler会根据不同的场景做不同的实例化
    Handler handler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        textView = findViewById(R.id.textview);
        button = findViewById(R.id.button);
        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                updateUI();
                //sendObj();
                //sendThreadHandler();
                //sendCallBack();
                //handlerPost();
                //sendDelayed();
                //sendAtTime();
                //sendSameWhenMsg();
            }
        });


    }


    //用handler来更新UI
    private void updateUI() {
        //Handler是绑定线程的，也就是说无论是哪个线程传过来的Message，都是主线程处理
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                //实际上super.handlerMessage(msg)是个空方法，里面啥都没，可以删掉这行
                super.handleMessage(msg);
                Log.i(TAG, "handleMessage: " + Thread.currentThread());
                if (msg.what == 0x12){
                    Bundle bundle = msg.getData();
                    String string = bundle.getString("msg", "");
                    textView.setText(string);
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: " + Thread.currentThread());

                Message message = handler.obtainMessage();
                //Message message = Message.obtain();

                Bundle bundle = new Bundle();
                bundle.putString("msg", "hello world");

                message.what = 0x12;
                message.setData(bundle);

                //用个sleep假设子线程在处理耗时时间，这一段不会让程序卡死，可是如果放在handlemessage内就会卡死
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                handler.sendMessage(message);
            }
        }).start();
    }



    //用handler来传送obj
    private void sendObj(){
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                super.handleMessage(msg);
                Log.i(TAG, "handleMessage: " + Thread.currentThread());
                if (msg.what == 0x13){
                    Num num = (Num)msg.obj;
                    Log.i(TAG, "handleMessage: " + num.getI());
                }
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: " + Thread.currentThread());

                Message message = handler.obtainMessage();
                //Message message = Message.obtain();

                //对一个int值做简单封装
                Num num = new Num(10);

                message.what = 0x13;
                message.obj = num;

                handler.sendMessage(message);
            }
        }).start();
    }



    //子线程的handler写法
    private void sendThreadHandler(){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //Looper.prepare生成一个新的子线程的Looper，不加这行就报错了
                //报错：java.lang.RuntimeException: Can't create handler inside thread Thread[Thread-2,5,main] that has not called Looper.prepare()
                Looper.prepare();

                //将handler放在子线程的内部，并且在构造函数中设定looper为子线程的looper。
                //事实上，并不需要加上这个参数，因为在构造函数中，handler自己就会调用Looper.myLooper()
                //也就是说，只要在子线程有Looper.prepare()后，再new Handler的话Handler就会自动设定子线程的looper
                handler = new Handler(Looper.myLooper()){
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        //可以看到Looper不为空，而且内部的Thread不是main
                        Log.i(TAG, "handleMessage: " + Thread.currentThread() + "    " + Looper.myLooper());
                    }
                };

                /*

                //这一段解除注释和不解除注释的结果不一样

                //这个就是强制设定为主线程的looper
                handler = new Handler(Looper.getMainLooper()){
                    @Override
                    public void handleMessage(@NonNull Message msg) {
                        super.handleMessage(msg);
                        //内部的Thread是main
                        Log.i(TAG, "handleMessage: " + Thread.currentThread() + "    " + Looper.myLooper());
                    }
                };

                */

                Message message = handler.obtainMessage();
                message.what = 0x14;
                message.arg1 = 111;
                handler.sendMessage(message);


                Looper.loop();

            }
        }).start();
    }


    //带有CallBack回调的方法。
    public void sendCallBack(){
        handler = new Handler(new Handler.Callback() {
            @Override
            public boolean handleMessage(@NonNull Message msg) {
                Log.i(TAG, "handleMessage: " + "CallBack");
                //针对不同的what值执行不同的内容。
                if (msg.what == 0x15)
                    return true;
                return false;
            }

        }){
            @Override
            public void handleMessage(@NonNull Message msg) {
                Log.i(TAG, "handleMessage: " + "NoCallBack");
                super.handleMessage(msg);
            }
        };


        new Thread(new Runnable() {
            @Override
            public void run() {

                Message message = handler.obtainMessage();
                //分别用两个不同的值尝试一次，结果会不一样
                message.what = 0x15;
                //message.what = 0x16;

                handler.sendMessage(message);

            }
        }).start();
    }


    //post方法
    public void handlerPost(){
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                //你会发现这个log并没有运行
                Log.i(TAG, "handleMessage: ");
                super.handleMessage(msg);
            }
        };


        new Thread(new Runnable() {
            @Override
            public void run() {
                Log.i(TAG, "run: "+ Thread.currentThread());


                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Log.i(TAG, "Runnable run: " + Thread.currentThread());
                    }
                });
            }
        }).start();
    }

    //尝试性的发送一个Delayed的消息
    public void sendDelayed(){
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                //数据相减可以很明显的得到一个接近3000ms的值，也就是差了3s
                Log.i(TAG, "handleMessage: " + System.currentTimeMillis());
                super.handleMessage(msg);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                Message message = handler.obtainMessage();
                //分别用两个不同的值尝试一次，结果会不一样
                message.what = 0x17;
                Log.i(TAG, "run: " + System.currentTimeMillis());
                handler.sendMessageDelayed(message, 3000);
            }
        }).start();
    }


    //以sendMessageAtTime为例，发送各种各样的信息，观察它们的输出顺序
    public void sendAtTime(){
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                //仔细观察其中的when和what值
                Log.i(TAG, "handleMessage: " + msg.getWhen() + "   " + msg.what);
                super.handleMessage(msg);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                //由于发送EmtpyMessage的速度很快，你可以看成同一时间发送了9这九条信息
                handler.sendEmptyMessage(34);
                handler.sendEmptyMessage(35);
                handler.sendEmptyMessage(36);
                handler.sendEmptyMessageAtTime(33, 3000);
                handler.sendEmptyMessageAtTime(31, 1000);
                handler.sendEmptyMessageAtTime(32, 2000);
                handler.sendEmptyMessageDelayed(37, 3000);
                handler.sendEmptyMessageDelayed(38, 2000);
                handler.sendEmptyMessageDelayed(39, 1000);

            }
        }).start();
    }


    //同时发送多个when值相同的Message，查看它们的输出顺序
    //结论：when值相同，后加入的在后面
    public void sendSameWhenMsg(){
        handler = new Handler(){
            @Override
            public void handleMessage(@NonNull Message msg) {
                //仔细观察其中的数据
                Log.i(TAG, "handleMessage: " + msg.getWhen() + "   " + msg.what);
                super.handleMessage(msg);
            }
        };

        new Thread(new Runnable() {
            @Override
            public void run() {
                handler.sendEmptyMessageAtTime(1, 1);
                handler.sendEmptyMessageAtTime(2, 1000);
                handler.sendEmptyMessageAtTime(31, 100);
                handler.sendEmptyMessageAtTime(32, 100);
                handler.sendEmptyMessageAtTime(33, 100);

            }
        }).start();
    }


}


