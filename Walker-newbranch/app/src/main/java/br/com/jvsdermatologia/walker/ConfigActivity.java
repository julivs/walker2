package br.com.jvsdermatologia.walker;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import br.com.jvsdermatologia.walker.Exceptions.ConfigException;

public class ConfigActivity extends AppCompatActivity {

    private EditText weight;
    private EditText height;
    private Switch soundSwitch;
    private Context context = this;
    private Switch gpsSwitch;
    private Switch turnoffSwitch;
    private EditText turnoffDistance;
    private TextView turnoffTitle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_config);

        weight = findViewById(R.id.editTextPeso);
        height = findViewById(R.id.editTextAltura);
        soundSwitch = findViewById(R.id.switchSound);
        gpsSwitch = findViewById(R.id.switchGPS);
        turnoffSwitch = findViewById(R.id.switchTurnoff);
        turnoffDistance = findViewById(R.id.editTextDesliga);
        turnoffTitle =findViewById(R.id.textViewDesliga);

        synchronized (this) {

            loadVariables();

            soundSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        Utils.getUtils().updateOneLineStringDataFile(context, Utils.getUtils().getSoundAlertVar(), "true");
                        Utils.getUtils().setMakeAlert(true);
                    } else {
                        Utils.getUtils().updateOneLineStringDataFile(context, Utils.getUtils().getSoundAlertVar(), "false");
                        Utils.getUtils().setMakeAlert(false);
                    }
                }
            });

            turnoffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        Utils.getUtils().updateOneLineStringDataFile(context, Utils.getUtils().getTurnOffActiveVar(), "true");
                        Utils.getUtils().setTurnoffDistActive(true);
                        showTextDist(true);

                    } else {
                        Utils.getUtils().updateOneLineStringDataFile(context, Utils.getUtils().getTurnOffActiveVar(), "false");
                        Utils.getUtils().setTurnoffDistActive(false);
                        showTextDist(false);
                    }
                }
            });

            gpsSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    if (isChecked){
                        Utils.getUtils().updateOneLineStringDataFile(context, Utils.getUtils().getGpsSensibVar(), "true");
                        Utils.getUtils().setHighSensibGPS(true);
                        Utils.getUtils().changeListenerGPS(5000, Utils.HIGH_SENSIB_GPS, context);
                    } else {
                        Utils.getUtils().updateOneLineStringDataFile(context, Utils.getUtils().getGpsSensibVar(), "false");
                        Utils.getUtils().setHighSensibGPS(false);
                        Utils.getUtils().changeListenerGPS(5000, Utils.LOW_SENSIB_GPS, context);
                    }
                }
            });
        }

    }

    public void onClickDefinir(View view) {

        try {
            int peso = Integer.parseInt(weight.getText().toString());
            int altura = Integer.parseInt(height.getText().toString());
            int dist = 0;
            if (turnoffSwitch.isChecked()) dist = Integer.parseInt(turnoffDistance.getText().toString());
            if (peso < 20 || peso > 150) throw new ConfigException("Peso");
            if (altura < 50 || altura > 250) throw new ConfigException("Altura");
            if ((dist < 500 || dist > 100000) && turnoffSwitch.isChecked()) throw new ConfigException("Distância");

            Utils.getUtils().setHeight(altura);
            Utils.getUtils().setWeight(peso);
            if (turnoffSwitch.isChecked()) Utils.getUtils().setDistanceTurnoff(dist);

            Utils.getUtils().updateOneLineStringDataFile(this,Utils.getUtils().getHeightVar(),Integer.toString(altura));
            Utils.getUtils().updateOneLineStringDataFile(this, Utils.getUtils().getWeightVar(), Integer.toString(peso));
            if (turnoffSwitch.isChecked()) Utils.getUtils().updateOneLineStringDataFile(this, Utils.getUtils().getTurnOffDistVar(), Integer.toString(dist));

            finish();

        } catch (ConfigException e) {
            Toast.makeText(this,"Valor inválido: " + e.getMessage(),Toast.LENGTH_LONG).show();
            e.printStackTrace();
        } catch (NumberFormatException e) {
            Toast.makeText(this,"Valor inválido.",Toast.LENGTH_LONG).show();
            e.printStackTrace();
        }
    }

    private void loadVariables() {

        String height = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getHeightVar());
        String weight = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getWeightVar());
        String soundAlert = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getSoundAlertVar());
        String gpsSensib = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getGpsSensibVar());
        String turnOffActive = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getTurnOffActiveVar());
        String turnOffDist = Utils.getUtils().getOneLineStringDataFile(this, Utils.getUtils().getTurnOffDistVar());

        try {
            if (height != null) {
                Utils.getUtils().setHeight(Integer.parseInt(height));
                this.height.setText(height);
            }
            if (weight != null) {
                Utils.getUtils().setWeight(Integer.parseInt(weight));
                this.weight.setText(weight);
            }
            if (turnOffDist != null) {
                Utils.getUtils().setDistanceTurnoff(Integer.parseInt(turnOffDist));
                this.turnoffDistance.setText(turnOffDist);
            }
            if (turnOffActive != null) {
                Utils.getUtils().setTurnoffDistActive(Boolean.parseBoolean(turnOffActive));
                this.turnoffSwitch.setChecked(Boolean.parseBoolean(turnOffActive));
                showTextDist(Boolean.parseBoolean(turnOffActive));
            } else {
                Utils.getUtils().setTurnoffDistActive(false);
                this.turnoffSwitch.setChecked(false);
                showTextDist(false);

            }
            if (soundAlert != null) {
                Utils.getUtils().setMakeAlert(Boolean.parseBoolean(soundAlert));
                this.soundSwitch.setChecked(Boolean.parseBoolean(soundAlert));
            } else {
                Utils.getUtils().setMakeAlert(true);
                this.soundSwitch.setChecked(true);
            }
            if (gpsSensib != null) {
                Utils.getUtils().setHighSensibGPS(Boolean.parseBoolean(gpsSensib));
                this.gpsSwitch.setChecked(Boolean.parseBoolean(gpsSensib));
            } else {
                Utils.getUtils().setHighSensibGPS(false);
                this.gpsSwitch.setChecked(false);
            }
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    private void showTextDist(boolean mostra) {
        if (mostra) {
            turnoffDistance.setVisibility(View.VISIBLE);
            turnoffTitle.setVisibility(View.VISIBLE);
        } else {
            turnoffDistance.setVisibility(View.GONE);
            turnoffTitle.setVisibility(View.GONE);
        }
    }
}