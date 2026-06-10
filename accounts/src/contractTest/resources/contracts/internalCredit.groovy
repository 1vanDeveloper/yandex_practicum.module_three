package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should credit to account"
    request {
        method 'POST'
        url '/accounts/internal/credit'
        headers {
            contentType(applicationJson())
        }
        body([
            login: "test_user",
            amount: 150.00
        ])
    }
    response {
        status 200
    }
}
