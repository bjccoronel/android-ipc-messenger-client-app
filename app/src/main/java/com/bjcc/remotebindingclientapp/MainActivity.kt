package com.bjcc.remotebindingclientapp

import android.content.ComponentName
import android.content.Intent
import android.content.ServiceConnection
import android.os.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bjcc.remotebindingclientapp.databinding.ActivityMainBinding
import timber.log.Timber

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    private var isServiceBound = false

    private var randomNumberValue = 0

    private var randomValueRequestMessenger: Messenger? = null

    private var randomNValueReceiveMessenger: Messenger? = null

    private var serviceIntent: Intent? = null

    inner class ReceiveRandomNumberHandler : Handler() {
        override fun handleMessage(msg: Message) {
            randomNumberValue = 0

            when (msg.what) {
                RandomValueFlag.GET_RANDOM_NUMBER.value -> {
                    randomNumberValue = msg.arg1
                    binding.tvValue.text = "Random number: $randomNumberValue"
                }
                RandomValueFlag.GET_RANDOM_COLOR.value -> {
                    binding.btnGetRandomColor.setBackgroundColor(msg.arg1);
                }
            }
            super.handleMessage(msg)
        }
    }

    private var randomNumberServiceConnection: ServiceConnection? = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Toast.makeText(this@MainActivity, "SERVICE CONNECTED ", Toast.LENGTH_SHORT).show()
            Timber.d("SERVICE CONNECTED - Thread id: ${Thread.currentThread().id}")

            randomValueRequestMessenger = Messenger(service)
            randomNValueReceiveMessenger = Messenger(ReceiveRandomNumberHandler())
            isServiceBound = true
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Toast.makeText(this@MainActivity, "SERVICE DISCONNECTED ", Toast.LENGTH_SHORT).show()
            Timber.d("SERVICE DISCONNECTED - Thread id: ${Thread.currentThread().id}")

            randomValueRequestMessenger = null
            randomNValueReceiveMessenger = null
            isServiceBound = false
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
            .also { setContentView(it.root) }

        Timber.d("onCreate - Thread id: ${Thread.currentThread().id}")

        serviceIntent = Intent().apply {
            component = ComponentName(
                "com.bjcc.remotebindingserviceapp",
                "com.bjcc.remotebindingserviceapp.MyService"
            )
        }

        binding.apply {
            btnBindService.setOnClickListener {
                bindToRemoteService()
            }

            btnUnbindService.setOnClickListener {
                unbindFromRemoteService()
            }

            btnRandomNumber.setOnClickListener {
                fetchRandomValue(RandomValueFlag.GET_RANDOM_NUMBER)
            }

            btnGetRandomColor.setOnClickListener {
                fetchRandomValue(RandomValueFlag.GET_RANDOM_COLOR)
            }
        }
    }

    private fun bindToRemoteService() {
        bindService(serviceIntent, randomNumberServiceConnection!!, BIND_AUTO_CREATE)
        Toast.makeText(this, "Service bound", Toast.LENGTH_SHORT).show()
    }

    private fun unbindFromRemoteService() {
        if (isServiceBound) {
            unbindService(randomNumberServiceConnection!!)
            isServiceBound = false
            Toast.makeText(this, "Service Unbound", Toast.LENGTH_SHORT).show()
        }
    }

    private fun fetchRandomValue(flag: RandomValueFlag) {
        if (randomValueRequestMessenger != null) {
            if (isServiceBound) {
                try {
                    val requestMessage = Message.obtain(null, flag.value)
                    requestMessage.replyTo = randomNValueReceiveMessenger

                    randomValueRequestMessenger!!.send(requestMessage)
                } catch (e: RemoteException) {
                    Timber.d("error: ${e.localizedMessage} - Thread id: ${Thread.currentThread().id}")
                }
            } else {
                Timber.d("Service Unbound, can't $flag - Thread id: ${Thread.currentThread().id}")
            }
        } else {
            Timber.d("randomValueRequestMessenger is null - Thread id: ${Thread.currentThread().id}")
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        randomNumberServiceConnection = null
    }

    enum class RandomValueFlag(val value: Int) {
        GET_RANDOM_NUMBER(0),
        GET_RANDOM_COLOR(1)
    }
}

