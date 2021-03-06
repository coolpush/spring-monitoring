package spring.monitoring.api;


import java.security.Principal;
import java.util.List;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import spring.monitoring.NotFoundException;
import spring.monitoring.model.Feed;
import spring.monitoring.service.FeedService;

@RestController
@RequestMapping("/feed")
public class FeedController {

  private final FeedService service;

  private final Counter counter;

  public FeedController(FeedService service, MeterRegistry meterRegistry) {
    this.service = service;
    counter = Counter.builder("feed-counter")
      .description("indicate new feed count")
      .tags(List.of(Tag.of("module", "account")))
      .register(meterRegistry);
  }

  @GetMapping
  public ResponseEntity<Iterable<Feed>> list(Principal principal) {
    return ResponseEntity.ok(service.list(principal.getName()));
  }

  @GetMapping("/{feedId}")
  @PreAuthorize("hasRole('ROLE_USER')")
  public ResponseEntity<Feed> detail(@PathVariable Long feedId) {
    return service.detail(feedId)
      .map(ResponseEntity::ok)
      .orElseThrow(NotFoundException::new);
  }

  @PostMapping
  @PreAuthorize("hasRole('ROLE_USER')")
  public ResponseEntity<Feed> create(Principal principal, @RequestBody Feed request) {
    var feed = service.create(principal.getName(), request);

    counter.increment();

    return ResponseEntity.created(
      MvcUriComponentsBuilder.fromController(getClass())
        .path("/feed/{feedId}")
        .build(feed.getId())
    ).body(feed);
  }

  @PostMapping("/{feedId}")
  @PreAuthorize("hasRole('ROLE_USER')")
  public ResponseEntity<Feed> modify(@PathVariable Long feedId, @RequestBody Feed request) {
    return ResponseEntity.created(
      ServletUriComponentsBuilder.fromCurrentRequestUri().build(feedId)
    ).body(service.modify(feedId, request));
  }

  @DeleteMapping("/{feedId}")
  @PreAuthorize("hasRole('ROLE_USER')")
  public ResponseEntity delete(@PathVariable Long feedId) {
    service.remove(feedId);
    return ResponseEntity.noContent().build();
  }
}
