package co.cetad.umas.operation.infrastructure.ugcs.config;

import lombok.Data;
import org.springframework.stereotype.Component;

@Data
@Component
public class CommandUgcsProperties {

    private String home = "return_to_home";

    private String takeOff = "takeoff_command";

    private String land = "land_command";

    private String emergencyLand = "emergency_land";

    private String pause = "pause_route";

    private String resume = "resume_route";

}
