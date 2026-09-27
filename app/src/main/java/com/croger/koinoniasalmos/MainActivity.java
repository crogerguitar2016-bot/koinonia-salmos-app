package com.croger.koinoniasalmos;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class MainActivity extends Activity {

    private static final int REQUEST_VCF = 1001;

    private static final String PREFS = "koinonia_prefs";
    private static final String PREF_VCF_URI = "vcf_uri";

    private Button botaoImportar;
    private Button botaoSalmo;
    private Button botaoIniciar;

    private TextView status;
    private TextView listaContatos;

    private final List<Contato> contatosKoinonia = new ArrayList<>();


    static class Contato {

        String nome;
        String nomeCompleto;
        String numero;

        Contato(
                String nome,
                String nomeCompleto,
                String numero
        ) {
            this.nome = nome;
            this.nomeCompleto = nomeCompleto;
            this.numero = numero;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        criarInterface();

        tentarCarregarArquivoSalvo();
    }


    // ========================================================
    // INTERFACE
    // ========================================================

    private void criarInterface() {

        int padding = dp(20);

        LinearLayout conteudo = new LinearLayout(this);
        conteudo.setOrientation(LinearLayout.VERTICAL);
        conteudo.setPadding(
                padding,
                padding,
                padding,
                padding
        );

        TextView titulo = new TextView(this);
        titulo.setText("KOINONIA — SALMOS");
        titulo.setTextSize(26);
        titulo.setTypeface(null, Typeface.BOLD);
        titulo.setGravity(Gravity.CENTER);

        TextView subtitulo = new TextView(this);
        subtitulo.setText(
                "\nAplicativo independente\n"
        );
        subtitulo.setTextSize(18);
        subtitulo.setGravity(Gravity.CENTER);

        botaoImportar = new Button(this);
        botaoImportar.setText("IMPORTAR CONTATOS");

        botaoSalmo = new Button(this);
        botaoSalmo.setText("ESCOLHER SALMO");
        botaoSalmo.setEnabled(false);

        botaoIniciar = new Button(this);
        botaoIniciar.setText("INICIAR MENSAGENS");
        botaoIniciar.setEnabled(false);

        status = new TextView(this);
        status.setText(
                "\nNenhum arquivo de contatos carregado."
        );
        status.setTextSize(17);
        status.setGravity(Gravity.CENTER);

        listaContatos = new TextView(this);
        listaContatos.setTextSize(17);
        listaContatos.setPadding(
                0,
                dp(16),
                0,
                dp(40)
        );

        conteudo.addView(
                titulo,
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        conteudo.addView(subtitulo);
        conteudo.addView(botaoImportar);
        conteudo.addView(botaoSalmo);
        conteudo.addView(botaoIniciar);
        conteudo.addView(status);
        conteudo.addView(listaContatos);

        ScrollView scroll = new ScrollView(this);

        scroll.addView(
                conteudo,
                new ScrollView.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                )
        );

        setContentView(scroll);


        botaoImportar.setOnClickListener(v -> escolherArquivoVcf());

        botaoSalmo.setOnClickListener(v -> {
            Toast.makeText(
                    this,
                    "Escolha de Salmo será ativada na próxima etapa.",
                    Toast.LENGTH_SHORT
            ).show();
        });
    }


    // ========================================================
    // ESCOLHER VCF
    // ========================================================

    private void escolherArquivoVcf() {

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.addCategory(Intent.CATEGORY_OPENABLE);

        intent.setType("*/*");

        intent.putExtra(
                Intent.EXTRA_MIME_TYPES,
                new String[]{
                        "text/vcard",
                        "text/x-vcard",
                        "text/plain",
                        "application/octet-stream"
                }
        );

        startActivityForResult(
                intent,
                REQUEST_VCF
        );
    }


    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(
                requestCode,
                resultCode,
                data
        );

        if (
                requestCode == REQUEST_VCF
                && resultCode == RESULT_OK
                && data != null
                && data.getData() != null
        ) {

            Uri uri = data.getData();

            try {

                int flags =
                        data.getFlags()
                                & (
                                Intent.FLAG_GRANT_READ_URI_PERMISSION
                                | Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                        );

                getContentResolver()
                        .takePersistableUriPermission(
                                uri,
                                flags & Intent.FLAG_GRANT_READ_URI_PERMISSION
                        );

            } catch (Exception ignored) {
            }

            getSharedPreferences(
                    PREFS,
                    MODE_PRIVATE
            )
                    .edit()
                    .putString(
                            PREF_VCF_URI,
                            uri.toString()
                    )
                    .apply();

            carregarVcf(uri);
        }
    }


    // ========================================================
    // CARREGAR AUTOMATICAMENTE
    // ========================================================

    private void tentarCarregarArquivoSalvo() {

        SharedPreferences prefs =
                getSharedPreferences(
                        PREFS,
                        MODE_PRIVATE
                );

        String salvo =
                prefs.getString(
                        PREF_VCF_URI,
                        null
                );

        if (salvo == null) {
            return;
        }

        try {

            carregarVcf(
                    Uri.parse(salvo)
            );

        } catch (Exception e) {

            status.setText(
                    "\nArquivo anterior não está mais disponível.\n"
                            + "Toque em IMPORTAR CONTATOS."
            );
        }
    }


    // ========================================================
    // LER ARQUIVO
    // ========================================================

    private void carregarVcf(Uri uri) {

        try {

            InputStream input =
                    getContentResolver()
                            .openInputStream(uri);

            if (input == null) {
                throw new Exception(
                        "Não foi possível abrir o arquivo."
                );
            }

            BufferedReader leitor =
                    new BufferedReader(
                            new InputStreamReader(
                                    input,
                                    StandardCharsets.UTF_8
                            )
                    );

            StringBuilder bruto =
                    new StringBuilder();

            String linha;

            while (
                    (linha = leitor.readLine())
                            != null
            ) {

                bruto
                        .append(linha)
                        .append("\n");
            }

            leitor.close();

            processarVcf(
                    bruto.toString()
            );

        } catch (Exception e) {

            contatosKoinonia.clear();

            botaoSalmo.setEnabled(false);
            botaoIniciar.setEnabled(false);

            status.setText(
                    "\nErro ao ler o arquivo de contatos."
            );

            listaContatos.setText(
                    "\n" + e.getMessage()
            );
        }
    }


    // ========================================================
    // PROCESSAR VCF
    // ========================================================

    private void processarVcf(String texto) {

        contatosKoinonia.clear();

        texto = desdobrarLinhas(
                texto
        );

        Pattern padrao =
                Pattern.compile(
                        "BEGIN:VCARD(.*?)END:VCARD",
                        Pattern.CASE_INSENSITIVE
                                | Pattern.DOTALL
                );

        Matcher matcher =
                padrao.matcher(texto);

        Set<String> numerosUsados =
                new HashSet<>();

        while (matcher.find()) {

            String cartao =
                    matcher.group(1);

            String nomeCompleto = "";

            List<String> telefones =
                    new ArrayList<>();

            String[] linhas =
                    cartao.split("\n");

            for (String linha : linhas) {

                String superior =
                        linha.toUpperCase(
                                Locale.ROOT
                        );

                if (
                        superior.startsWith("FN:")
                        || superior.startsWith("FN;")
                ) {

                    nomeCompleto =
                            decodificarValor(
                                    linha
                            );
                }

                if (
                        superior.matches(
                                "^(ITEM\\d+\\.)?TEL(;|:).*"
                        )
                ) {

                    String telefone =
                            normalizarNumero(
                                    decodificarValor(
                                            linha
                                    )
                            );

                    if (telefone != null) {
                        telefones.add(
                                telefone
                        );
                    }
                }
            }

            if (nomeCompleto.isEmpty()) {
                continue;
            }

            if (
                    !nomeCompleto
                            .trim()
                            .toLowerCase(Locale.ROOT)
                            .matches(
                                    ".*(?:^|\\s)koinonia$"
                            )
            ) {
                continue;
            }

            if (telefones.isEmpty()) {
                continue;
            }

            String primeiroNome =
                    limparPrimeiroNome(
                            nomeCompleto
                    );

            if (primeiroNome.isEmpty()) {
                continue;
            }

            String telefone =
                    telefones.get(0);

            if (
                    numerosUsados.contains(
                            telefone
                    )
            ) {
                continue;
            }

            numerosUsados.add(
                    telefone
            );

            contatosKoinonia.add(
                    new Contato(
                            primeiroNome,
                            nomeCompleto,
                            telefone
                    )
            );
        }


        Collections.sort(
                contatosKoinonia,
                Comparator.comparing(
                        c -> c.nome.toLowerCase(
                                Locale.ROOT
                        )
                )
        );


        mostrarResultado();
    }


    // ========================================================
    // LINHAS CONTINUADAS
    // ========================================================

    private String desdobrarLinhas(
            String texto
    ) {

        texto = texto
                .replace("\r\n", "\n")
                .replace("\r", "\n");

        String[] linhas =
                texto.split(
                        "\n",
                        -1
                );

        List<String> resultado =
                new ArrayList<>();

        for (String linha : linhas) {

            if (
                    !resultado.isEmpty()
                    && (
                    linha.startsWith(" ")
                            || linha.startsWith("\t")
            )
            ) {

                int ultimo =
                        resultado.size() - 1;

                resultado.set(
                        ultimo,
                        resultado.get(ultimo)
                                + linha.substring(1)
                );

                continue;
            }

            if (
                    !resultado.isEmpty()
                    && resultado
                    .get(
                            resultado.size() - 1
                    )
                    .endsWith("=")
                    && resultado
                    .get(
                            resultado.size() - 1
                    )
                    .toUpperCase(Locale.ROOT)
                    .contains(
                            "QUOTED-PRINTABLE"
                    )
            ) {

                int ultimo =
                        resultado.size() - 1;

                String anterior =
                        resultado.get(ultimo);

                resultado.set(
                        ultimo,
                        anterior.substring(
                                0,
                                anterior.length() - 1
                        ) + linha
                );

                continue;
            }

            resultado.add(linha);
        }

        StringBuilder finalTexto =
                new StringBuilder();

        for (String item : resultado) {

            finalTexto
                    .append(item)
                    .append("\n");
        }

        return finalTexto.toString();
    }


    // ========================================================
    // DECODIFICAR VALOR
    // ========================================================

    private String decodificarValor(
            String linha
    ) {

        int doisPontos =
                linha.indexOf(':');

        if (doisPontos < 0) {
            return "";
        }

        String cabecalho =
                linha.substring(
                        0,
                        doisPontos
                );

        String valor =
                linha.substring(
                        doisPontos + 1
                );

        if (
                cabecalho
                        .toUpperCase(Locale.ROOT)
                        .contains(
                                "QUOTED-PRINTABLE"
                        )
        ) {

            try {

                byte[] bytes =
                        decodeQuotedPrintable(
                                valor
                        );

                Charset charset =
                        StandardCharsets.UTF_8;

                Matcher m =
                        Pattern
                                .compile(
                                        "CHARSET=([^;:]+)",
                                        Pattern.CASE_INSENSITIVE
                                )
                                .matcher(
                                        cabecalho
                                );

                if (m.find()) {

                    try {

                        charset =
                                Charset.forName(
                                        m.group(1)
                                );

                    } catch (Exception ignored) {
                    }
                }

                valor =
                        new String(
                                bytes,
                                charset
                        );

            } catch (Exception ignored) {
            }
        }

        return valor
                .replace("\\n", " ")
                .replace("\\N", " ")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\")
                .trim();
    }


    private byte[] decodeQuotedPrintable(
            String texto
    ) {

        ByteArrayOutputStream saida =
                new ByteArrayOutputStream();

        for (
                int i = 0;
                i < texto.length();
                i++
        ) {

            char c = texto.charAt(i);

            if (
                    c == '='
                    && i + 2 < texto.length()
            ) {

                String hex =
                        texto.substring(
                                i + 1,
                                i + 3
                        );

                try {

                    saida.write(
                            Integer.parseInt(
                                    hex,
                                    16
                            )
                    );

                    i += 2;

                    continue;

                } catch (Exception ignored) {
                }
            }

            byte[] bytes =
                    String.valueOf(c)
                            .getBytes(
                                    StandardCharsets.UTF_8
                            );

            saida.write(
                    bytes,
                    0,
                    bytes.length
            );
        }

        return saida.toByteArray();
    }


    // ========================================================
    // TELEFONE
    // ========================================================

    private String normalizarNumero(
            String numero
    ) {

        if (numero == null) {
            return null;
        }

        numero =
                numero.replaceAll(
                        "\\D",
                        ""
                );

        if (numero.isEmpty()) {
            return null;
        }

        if (
                numero.startsWith("00")
        ) {

            numero =
                    numero.substring(2);
        }

        if (
                numero.length() == 10
                || numero.length() == 11
        ) {

            numero =
                    "55" + numero;
        }

        if (
                numero.length() < 10
        ) {

            return null;
        }

        return numero;
    }


    // ========================================================
    // PRIMEIRO NOME
    // ========================================================

    private String limparPrimeiroNome(
            String nomeCompleto
    ) {

        String nome =
                nomeCompleto.replaceFirst(
                        "(?i)\\s+Koinonia\\s*$",
                        ""
                ).trim();

        if (nome.isEmpty()) {
            return "";
        }

        String primeiro =
                nome.split("\\s+")[0];

        primeiro =
                primeiro.replaceFirst(
                        "^[^\\p{L}\\p{N}]+",
                        ""
                );

        primeiro =
                primeiro.replaceFirst(
                        "[^\\p{L}\\p{N}]+$",
                        ""
                );

        if (primeiro.isEmpty()) {
            return "";
        }

        return primeiro.substring(
                0,
                1
        ).toUpperCase()
                + primeiro.substring(1);
    }


    // ========================================================
    // MOSTRAR CONTATOS
    // ========================================================

    private void mostrarResultado() {

        int quantidade =
                contatosKoinonia.size();

        status.setText(
                "\nCONTATOS KOINONIA ENCONTRADOS: "
                        + quantidade
        );

        StringBuilder lista =
                new StringBuilder();

        for (
                int i = 0;
                i < contatosKoinonia.size();
                i++
        ) {

            Contato contato =
                    contatosKoinonia.get(i);

            lista.append(
                    i + 1
            );

            lista.append(
                    " - "
            );

            lista.append(
                    contato.nome
            );

            lista.append(
                    "\n"
            );
        }

        listaContatos.setText(
                lista.toString()
        );

        botaoSalmo.setEnabled(
                quantidade > 0
        );

        botaoIniciar.setEnabled(false);

        if (quantidade > 0) {

            Toast.makeText(
                    this,
                    quantidade
                            + " contatos Koinonia encontrados.",
                    Toast.LENGTH_LONG
            ).show();
        }
    }


    private int dp(int valor) {

        float densidade =
                getResources()
                        .getDisplayMetrics()
                        .density;

        return (int) (
                valor * densidade
        );
    }
}
