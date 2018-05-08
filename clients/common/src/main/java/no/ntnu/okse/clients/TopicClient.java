package no.ntnu.okse.clients;

import com.beust.jcommander.Parameter;
import java.util.List;

public abstract class TopicClient extends CommandClient {

  @Parameter(names = {"--topic", "-t"}, description = "Topic", required = true)
  public List<String> topics;

}
