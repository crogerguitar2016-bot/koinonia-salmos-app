package com.croger.koinoniasalmos;

import android.app.*;
import android.app.Service;
import android.content.Intent;
import android.net.Uri;
import android.os.*;

import java.util.ArrayList;
import java.util.Calendar;


public class MessageService extends Service {

    private static final String CANAL =
            "koinonia_envio";

    private static final int NOTIFICACAO =
            1201;

    private static final long INTERVALO =
            5000;


    private Handler handler;


    private ArrayList<String> nomes;
    private ArrayList<String> numeros;

    private ArrayList<Integer> salmos;
    private ArrayList<Integer> versiculos;

    private ArrayList<String> textos;


    private int atual = 0;

    private boolean cancelado = false;


    @Override
    public void onCreate() {

        super.onCreate();


        handler =
                new Handler(
                        Looper.getMainLooper()
                );


        criarCanal();
    }


    @Override
    public int onStartCommand(
            Intent intent,
            int flags,
            int startId
    ) {

        if (
                intent != null
                && "CANCELAR".equals(
                intent.getAction()
        )
        ) {

            cancelar();

            return START_NOT_STICKY;
        }


        if (intent == null) {

            stopSelf();

            return START_NOT_STICKY;
        }


        handler.removeCallbacksAndMessages(
                null
        );


        nomes =
                intent.getStringArrayListExtra(
                        "nomes"
                );


        numeros =
                intent.getStringArrayListExtra(
                        "numeros"
                );


        salmos =
                intent.getIntegerArrayListExtra(
                        "salmos"
                );


        versiculos =
                intent.getIntegerArrayListExtra(
                        "versiculos"
                );


        textos =
                intent.getStringArrayListExtra(
                        "textos"
                );


        if (
                nomes == null
                || numeros == null
                || salmos == null
                || versiculos == null
                || textos == null
                || nomes.isEmpty()
        ) {

            stopSelf();

            return START_NOT_STICKY;
        }


        atual = 0;

        cancelado = false;


        startForeground(
                NOTIFICACAO,
                criarNotificacao(
                        "Preparando mensagens..."
                )
        );


        handler.post(
                this::abrirAtual
        );


        return START_NOT_STICKY;
    }


    private void abrirAtual() {

        if (cancelado) {

            stopSelf();

            return;
        }


        if (
                atual >= nomes.size()
        ) {

            finalizar();

            return;
        }


        atualizarNotificacao();


        String mensagem =
                montarMensagem(
                        nomes.get(atual),
                        salmos.get(atual),
                        versiculos.get(atual),
                        textos.get(atual)
                );


        abrirWhatsApp(
                numeros.get(atual),
                mensagem
        );


        atual++;


        if (
                atual < nomes.size()
        ) {

            handler.postDelayed(
                    this::abrirAtual,
                    INTERVALO
            );

        } else {

            handler.postDelayed(
                    this::finalizar,
                    INTERVALO
            );
        }
    }


    private String montarMensagem(
            String nome,
            int salmo,
            int versiculo,
            String texto
    ) {

        return nome
                + ", "
                + saudacao()
                + "! "
                + "Um versículo especial para você."
                + "\n\n\n"
                + "Salmo "
                + salmo
                + ":"
                + versiculo
                + " — "
                + texto
                + " "
                + "(BLIVRE)";
    }


    private String saudacao() {

        int hora =
                Calendar
                        .getInstance()
                        .get(
                                Calendar.HOUR_OF_DAY
                        );


        if (
                hora >= 5
                && hora < 12
        ) {
            return "bom dia";
        }


        if (
                hora >= 12
                && hora < 18
        ) {
            return "boa tarde";
        }


        return "boa noite";
    }


    private void abrirWhatsApp(
            String numero,
            String texto
    ) {

        try {

            String url =
                    "https://wa.me/"
                            + numero
                            + "?text="
                            + Uri.encode(
                            texto
                    );


            Intent intent =
                    new Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse(url)
                    );


            intent.setPackage(
                    "com.whatsapp.w4b"
            );


            intent.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
            );


            startActivity(
                    intent
            );

        } catch (Exception ignored) {
        }
    }


    private void criarCanal() {

        if (
                Build.VERSION.SDK_INT >= 26
        ) {

            NotificationChannel canal =
                    new NotificationChannel(
                            CANAL,
                            "Envio Koinonia",
                            NotificationManager
                                    .IMPORTANCE_LOW
                    );


            canal.setDescription(
                    "Fila de mensagens do Koinonia Salmos"
            );


            NotificationManager nm =
                    getSystemService(
                            NotificationManager.class
                    );


            nm.createNotificationChannel(
                    canal
            );
        }
    }


    private Notification criarNotificacao(
            String texto
    ) {

        Intent cancelarIntent =
                new Intent(
                        this,
                        MessageService.class
                );


        cancelarIntent.setAction(
                "CANCELAR"
        );


        PendingIntent cancelarPending =
                PendingIntent.getService(
                        this,
                        0,
                        cancelarIntent,

                        PendingIntent.FLAG_UPDATE_CURRENT
                                | PendingIntent.FLAG_IMMUTABLE
                );


        Notification.Builder builder;


        if (
                Build.VERSION.SDK_INT >= 26
        ) {

            builder =
                    new Notification.Builder(
                            this,
                            CANAL
                    );

        } else {

            builder =
                    new Notification.Builder(
                            this
                    );
        }


        return builder

                .setSmallIcon(
                        android.R.drawable
                                .ic_dialog_info
                )

                .setContentTitle(
                        "Koinonia Salmos"
                )

                .setContentText(
                        texto
                )

                .setOngoing(
                        true
                )

                .addAction(
                        android.R.drawable
                                .ic_menu_close_clear_cancel,

                        "Cancelar",

                        cancelarPending
                )

                .build();
    }


    private void atualizarNotificacao() {

        String texto =
                (atual + 1)
                        + "/"
                        + nomes.size()
                        + " — "
                        + nomes.get(atual);


        NotificationManager nm =
                (NotificationManager)
                        getSystemService(
                                NOTIFICATION_SERVICE
                        );


        nm.notify(
                NOTIFICACAO,
                criarNotificacao(
                        texto
                )
        );
    }


    private void cancelar() {

        cancelado = true;


        handler.removeCallbacksAndMessages(
                null
        );


        stopForeground(
                true
        );


        stopSelf();
    }


    private void finalizar() {

        handler.removeCallbacksAndMessages(
                null
        );


        stopForeground(
                true
        );


        stopSelf();
    }


    @Override
    public void onDestroy() {

        handler.removeCallbacksAndMessages(
                null
        );


        super.onDestroy();
    }


    @Override
    public IBinder onBind(
            Intent intent
    ) {

        return null;
    }
}
