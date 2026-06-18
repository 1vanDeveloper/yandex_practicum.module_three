package contracts

import org.springframework.cloud.contract.spec.Contract

Contract.make {
    description "should get current user account"
    request {
        method 'GET'
        url '/accounts/me'
    }
    response {
        status 200
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            login: "test_user",
            firstName: "Test",
            lastName: "User",
            birthDate: "1990-05-15",
            amount: 1000.00
        ])
    }
}
