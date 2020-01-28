package application;

import com.pubnub.api.PNConfiguration;
import com.pubnub.api.PubNub;
import com.pubnub.api.callbacks.PNCallback;
import com.pubnub.api.callbacks.SubscribeCallback;
import com.pubnub.api.models.consumer.PNPublishResult;
import com.pubnub.api.models.consumer.PNStatus;
import com.pubnub.api.models.consumer.presence.PNHereNowChannelData;
import com.pubnub.api.models.consumer.presence.PNHereNowOccupantData;
import com.pubnub.api.models.consumer.presence.PNHereNowResult;
import com.pubnub.api.models.consumer.pubsub.PNMessageResult;
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult;
import com.pubnub.api.models.consumer.pubsub.PNSignalResult;
import com.pubnub.api.models.consumer.pubsub.message_actions.PNMessageActionResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNMembershipResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNSpaceResult;
import com.pubnub.api.models.consumer.pubsub.objects.PNUserResult;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import java.net.URL;
import java.util.*;

public class ChatController implements Initializable {
    @FXML
    private TextField sentMessages;
    @FXML
    private TextArea receiveMessages;
    @FXML
    private Label commStat;
    @FXML
    private Label occupancy;
    @FXML
    private TextArea subscriberList;

    private PNConfiguration pnConfiguration;
    private PubNub pubnub;
    private String channelName = "channelName";
    private String timeSent;


    public void initialize(URL location, ResourceBundle resources) {
        commStat.setText("Not connected to any channel.");
        commStat.setStyle("-fx-text-fill: red");

        sentMessages.setOnKeyPressed(new EventHandler<KeyEvent>() {
            @Override
            public void handle(KeyEvent event) {
                if (event.getCode() == KeyCode.ENTER) {
                    sendMessage();
                    event.consume();
                }
            }
        });

    }

    @FXML
    void onSend(ActionEvent event) {
        sendMessage();
    }

    public void sendMessage() {
        if (pnConfiguration == null) {
            if (receiveMessages.getText().length() == 0) {
                receiveMessages.setText("Current status not subscribed!");
            } else {
                receiveMessages.setText(receiveMessages.getText() + '\n' + "Current status not subscribed!");
            }
            receiveMessages.selectPositionCaret(receiveMessages.getLength());
            receiveMessages.deselect();
            return;
        }

        GregorianCalendar gCalendar = new GregorianCalendar();
        timeSent = "Time sent: "
                + gCalendar.get(Calendar.HOUR) + ":"
                + gCalendar.get(Calendar.MINUTE) + ":"
                + gCalendar.get(Calendar.SECOND) + ":"
                + gCalendar.get(Calendar.MILLISECOND);

        pubnub.publish()
                .message("\"" + sentMessages.getText() + "\"")
                .channel(channelName)
                .async(new PNCallback<PNPublishResult>() {
                    @Override
                    public void onResponse(PNPublishResult result, PNStatus status) {
                        if (!status.isError()) {
                            System.out.println("Message timetoken: " + result.getTimetoken());
                        } else {
                            status.getErrorData().getThrowable().printStackTrace();
                        }
                    }
                });
    }

    @FXML
    void onSubscribe(ActionEvent event) {
        if (pnConfiguration == null) {
            pnConfiguration = new PNConfiguration();
            pnConfiguration.setSubscribeKey("sub-key");
            pnConfiguration.setPublishKey("pub-key");
            pnConfiguration.setUuid("UUID_Name");

            pubnub = new PubNub(pnConfiguration);

            pubnub.addListener(new SubscribeCallback() {

                @Override
                public void status(PubNub pubnub, PNStatus pnStatus) {
                }

                @Override
                public void message(PubNub pubnub, PNMessageResult pnMessageResult) {
                    // print basic info about newly received messages
                    String recMsg;
                    System.out.println("Message channel: " + pnMessageResult.getChannel());
                    System.out.println("Message publisher: " + pnMessageResult.getPublisher());

                    GregorianCalendar gCalendar = new GregorianCalendar();
                    String timeReceived = "Time received: "
                            + gCalendar.get(Calendar.HOUR) + ":"
                            + gCalendar.get(Calendar.MINUTE) + ":"
                            + gCalendar.get(Calendar.SECOND) + ":"
                            + gCalendar.get(Calendar.MILLISECOND);

                    if (!pnMessageResult.getPublisher().equals(pnConfiguration.getUuid())) {
                        //messages received

                        if (receiveMessages.getText().length() == 0) {
                            recMsg = "From " + pnMessageResult.getPublisher().replace("UUID: ", "") + ": "
                                    + pnMessageResult.getMessage().getAsString() + " [" + timeReceived + "]";
                        } else {
                            recMsg = receiveMessages.getText() + '\n' + "From " + pnMessageResult.getPublisher().replace("UUID: ", "")
                                    + ": " + pnMessageResult.getMessage().getAsString() + " [" + timeReceived + "]";
                        }

                        receiveMessages.setText(recMsg);
                    } else {
                        //messages sent
                        if (receiveMessages.getText().length() == 0) {
                            recMsg = "Me: " + pnMessageResult.getMessage().getAsString() + " [" + timeSent + "]";
                        } else {
                            recMsg = receiveMessages.getText() + '\n' + "Me: " + pnMessageResult.getMessage().getAsString() + " [" + timeSent + "]";
                        }
                    }

                    receiveMessages.setText(recMsg);
                    receiveMessages.selectPositionCaret(receiveMessages.getLength());
                    receiveMessages.deselect();
                }

                @Override
                public void presence(PubNub pubnub, PNPresenceEventResult pnPresenceEventResult) {
                    // print basic info about newly received presence events
                    System.out.println("Presence channel: " + pnPresenceEventResult.getChannel());
                    System.out.println("Presence event: " + pnPresenceEventResult.getEvent());
                    System.out.println("Presence uuid: " + pnPresenceEventResult.getUuid());

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    //Get occupancy of who's here now on the channel by UUID:
                    pubnub.hereNow()
                            // tailor the next two lines to example
                            .channels(Arrays.asList(channelName))
                            .includeUUIDs(true)
                            .async(new PNCallback<PNHereNowResult>() {
                                @Override
                                public void onResponse(PNHereNowResult result, PNStatus status) {
                                    if (status.isError()) {
                                        // handle error
                                        return;
                                    }

                                    for (PNHereNowChannelData channelData : result.getChannels().values()) {
                                        System.out.println("---");
                                        System.out.println("channel:" + channelData.getChannelName());
                                        System.out.println("occupancy: " + channelData.getOccupancy());
                                        System.out.println("occupants:");

                                        Platform.runLater(() -> {
                                            //update status label
                                            commStat.setText("Joined \"" + channelData.getChannelName() + "\"");
                                            commStat.setStyle("-fx-text-fill: green");

                                            //update the number of occupancy
                                            occupancy.setText(String.valueOf(channelData.getOccupancy()));
                                        });

                                        subscriberList.clear();
                                        for (PNHereNowOccupantData occupant : channelData.getOccupants()) {
                                            System.out.println("uuid: " + occupant.getUuid() + " state: " + occupant.getState());

                                            //update the list of subscriber's name
                                            subscriberList.setText(subscriberList.getText() + occupant.getUuid() + "\n");
                                        }
                                    }
                                }
                            });
                }

                @Override
                public void signal(PubNub pubnub, PNSignalResult pnSignalResult) {
                }

                @Override
                public void user(PubNub pubnub, PNUserResult pnUserResult) {
                }

                @Override
                public void space(PubNub pubnub, PNSpaceResult pnSpaceResult) {
                }

                @Override
                public void membership(PubNub pubnub, PNMembershipResult pnMembershipResult) {
                }

                @Override
                public void messageAction(PubNub pubNub, PNMessageActionResult pnMessageActionResult) {

                }
            });

            pubnub.subscribe()
                    .channels(Collections.singletonList(channelName))
                    .withPresence() // to receive presence events
                    .execute();
        }
    }

    @FXML
    void onUnSubscribe(ActionEvent event) {
        if (pnConfiguration != null) {
            pubnub.unsubscribe()
                    .channels(Collections.singletonList(channelName))
                    .execute();

            commStat.setText("Left \"" + channelName + "\"");
            commStat.setStyle("-fx-text-fill: blue");

            //zero out the number of occupancy since it is not already listening to any channel
            occupancy.setText("0");

            //empty the subscriber's list since it is not already listening to any channel
            subscriberList.clear();

            pnConfiguration = null;
        }
    }

    @FXML
    void onExit(ActionEvent event) {
        if (pubnub != null) {
            pubnub.destroy();
        }

        Platform.exit();
        System.exit(0);
    }
}
