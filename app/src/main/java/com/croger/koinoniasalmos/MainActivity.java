package com.croger.koinoniasalmos;

import android.app.Activity;
import android.os.Bundle;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int padding = dp(24);

        LinearLayout tela = new LinearLayout(this);
        tela.setOrientation(LinearLayout.VERTICAL);
        tela.setPadding(padding, padding, padding, padding);
        tela.setGravity(Gravity.CENTER_HORIZONTAL);

        TextView titulo = new TextView(this);
        titulo.setText("KOINONIA — SALMOS");
        titulo.setTextSize(26);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setGravity(Gravity.CENTER);

        TextView subtitulo = new TextView(this);
        subtitulo.setText(
                "\nAplicativo independente\n\n" +
                "Nesta primeira etapa estamos confirmando " +
                "que o APK abre sem depender do Termux."
        );
        subtitulo.setTextSize(18);
        subtitulo.setGravity(Gravity.CENTER);

        Button contatos = new Button(this);
        contatos.setText("IMPORTAR CONTATOS");
        contatos.setEnabled(false);

        Button salmo = new Button(this);
        salmo.setText("ESCOLHER SALMO");
        salmo.setEnabled(false);

        Button iniciar = new Button(this);
        iniciar.setText("INICIAR MENSAGENS");
        iniciar.setEnabled(false);

        tela.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        tela.addView(
                subtitulo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        tela.addView(contatos);
        tela.addView(salmo);
        tela.addView(iniciar);

        setContentView(tela);
    }

    private int dp(int valor) {
        float densidade = getResources()
                .getDisplayMetrics()
                .density;

        return (int) (valor * densidade);
    }
}
